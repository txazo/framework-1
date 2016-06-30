/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package leap.core;

import leap.core.instrument.AppInstrumentClass;
import leap.core.instrument.AppInstrumentation;
import leap.core.instrument.ClassDependency;
import leap.core.instrument.ClassDependencyResolver;
import leap.lang.Classes;
import leap.lang.Exceptions;
import leap.lang.Factory;
import leap.lang.annotation.Internal;
import leap.lang.exception.NestedClassNotFoundException;
import leap.lang.io.IO;
import leap.lang.logging.Log;
import leap.lang.logging.LogFactory;
import leap.lang.resource.Resource;
import leap.lang.resource.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

@Internal
public class AppClassLoader extends ClassLoader {

    private static final Log log = LogFactory.get(AppClassLoader.class);

    private static final ThreadLocal<Set<String>> beanClassNamesLocal = new ThreadLocal<>();

    private static ThreadLocal<Boolean> useParentLocal;
    private static AppClassLoader       INSTANCE;

    public static void dontUseParent() {
        useParentLocal = new ThreadLocal<>();
        useParentLocal.set(false);
    }

    public static AppClassLoader get() {
        return INSTANCE;
    }

    static AppClassLoader init(ClassLoader parent, AppConfig config)  {
        INSTANCE = new AppClassLoader(parent, config);
        return INSTANCE;
    }

    public static void addBeanClass(String name) {
        Set<String> names = beanClassNamesLocal.get();
        if(null == names) {
            names = new HashSet<>();
            beanClassNamesLocal.set(names);
        }
        names.add(name);
    }

    public static boolean isBeanClass(String name) {
        Set<String> names = beanClassNamesLocal.get();
        if(null == names) {
            return false;
        }

        return names.contains(name);
    }

    private final ClassLoader parent;
    private final AppConfig   config;
    private final String      basePackage;

    private final Set<String>             loadedUrls         = new HashSet<>();
    private final Set<String>             loadedNames        = new HashSet<>();
    private final AppInstrumentation      instrumentation    = Factory.newInstance(AppInstrumentation.class);
    private final ClassDependencyResolver dependencyResolver = Factory.newInstance(ClassDependencyResolver.class);
    private final Set<String>             instrumenting      = new HashSet<>();

    private boolean useParent = true;
    private Method  parentLoaderDefineClass;
    private Method  parentFindLoadedClass;

    private AppClassLoader(ClassLoader parent, AppConfig config) {
        this.parent      = parent;
        this.config      = config;
        this.basePackage = config.getBasePackage() + ".";

        if(null != useParentLocal) {
            this.useParent = useParentLocal.get();
        }

        try {
            parentLoaderDefineClass =
                    ClassLoader.class.getDeclaredMethod("defineClass",
                        new Class[] {String.class, byte[].class, int.class,int.class});
            parentLoaderDefineClass.setAccessible(true);

            parentFindLoadedClass =
                    ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            parentFindLoadedClass.setAccessible(true);
        } catch (Exception e) {
            throw Exceptions.uncheck(e);
        }

        instrumentation.init(config);

        loadAllClasses();
    }

    void done() {
        loadedUrls.clear();
        loadedNames.clear();
        instrumenting.clear();
        beanClassNamesLocal.remove();
    }

    @Override
    public URL getResource(String name) {
        return parent.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return parent.getResources(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return parent.getResourceAsStream(name);
    }

    private void loadAllClasses() {
        log.debug("Try instrument all classes in app configured resources.");
        config.getResources().forEach(resource -> {

            if(resource.exists()) {
                String filename = resource.getFilename();

                if(null != filename &&
                        filename.endsWith(Classes.CLASS_FILE_SUFFIX)) {

                    try {
                        instrumentClass(null, resource, true);
                    } catch (ClassNotFoundException e) {
                        throw new NestedClassNotFoundException(e);
                    }
                }
            }
        });
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        log.trace("Loading class '{}'...", name);

        Class<?> c = findLoadedClass(name);

        if (null == c) {
            c = this.findClass(name);
        }

        if (null == c) {
            log.trace("Load class '{}' by parent loader", name);
            c = parent.loadClass(name);
        }

        if (resolve) {
            resolveClass(c);
        }

        return c;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        if(loadedNames.contains(name)) {
            return null;
        }else{
            loadedNames.add(name);
        }

        if(useParent && isParentLoaded(name)) {
            log.trace("Class '{}' already loaded by parent", name);
            return null;
        }

        return instrumentClass(name);
    }

    private Class<?> instrumentClass(String name) throws ClassNotFoundException {
        if(isIgnore(name)) {
            return null;
        }

        Resource resource = Resources.getResource("classpath:" + name.replace('.', '/') + ".class");
        if(null == resource || !resource.exists()) {
            return null;
        }

        if(instrumenting.contains(name)) {
            log.info("Found cyclic instrumenting class '{}'", name);
            return instrumentClass(name, resource, false);
        }

        instrumenting.add(name);

        Class<?> c = instrumentClass(name, resource, true);

        instrumenting.remove(name);

        return c;
    }

    private Class<?> instrumentClass(String name, Resource resource, boolean depFirst) throws ClassNotFoundException {
        String url = resource.getURLString();
        if(loadedUrls.contains(url)) {
            return null;
        }

        InputStream is = null;
        try {
            is = resource.getInputStream();
            byte[] bytes = IO.readByteArray(is);

            if(useParent && depFirst) {
                ClassDependency dep = dependencyResolver.resolveDependentClassNames(resource, bytes);

                if(null != dep.getSuperClassName() && !"java.lang.Object".equals(dep.getSuperClassName())) {
                    log.trace("Loading super class '{}' of '{}'", dep.getSuperClassName(), dep.getClassName());
                    instrumentClass(dep.getSuperClassName());
                }

                if(!dep.getDependentClassNames().isEmpty()) {

                    log.trace("Loading {} dependent classes of '{}'...",
                            dep.getDependentClassNames().size(),
                            dep.getClassName());

                    for(String depClassName : dep.getDependentClassNames()) {
                        log.trace("Loading dependent class '{}'", depClassName);
                        instrumentClass(depClassName);
                    }

                }

                if(loadedUrls.contains(url)){
                    return null;
                }
            }

            //try instrument the class.
            AppInstrumentClass ic = instrumentation.tryInstrument(this, resource, bytes);
            if(null == ic && null == name) {
                return null;
            }

            loadedUrls.add(url);

            if(useParent) {
                if(null == ic) {
                    return null;
                }else{
                    name  = ic.getClassName();
                    bytes = ic.getClassData();
                }

                log.trace("Defining instrumented class '{}' use parent loader", name);
                Object[] args = new Object[]{name, bytes, 0, bytes.length};

                try {
                    return (Class<?>) parentLoaderDefineClass.invoke(parent, args);
                }catch(InvocationTargetException e) {
                    Throwable cause = e.getCause();

                    if(cause instanceof ClassFormatError) {
                        throw new IllegalStateException("Instrument error of '" + name + "'", cause);
                    }

                    if(cause instanceof  LinkageError) {
                        if (ic.isEnsure()) {
                            throw new IllegalStateException("Class '" + name + "' already loaded by '" +
                                    parent.getClass().getName() + "', cannot instrument it!");
                        } else {
                            log.warn("Cannot define the instrumented class '{}', it was loaded by parent loader", name);
                            return null;
                        }
                    }else{
                        throw new ClassNotFoundException(name, cause);
                    }
                }catch(Exception e) {
                    throw new ClassNotFoundException(name, e);
                }
            }else{
                if(null != ic) {
                    name  = ic.getClassName();
                    bytes = ic.getClassData();
                }
                log.trace("Defining class '{}' use app loader", name);
                return defineClass(name, bytes, 0, bytes.length);
            }
        }catch(IOException e) {
            throw new ClassNotFoundException(name, e);
        }finally{
            IO.close(is);
        }
    }

    private boolean isParentLoaded(String className) {
        try {
            return null != parentFindLoadedClass.invoke(parent, className);
        } catch (Exception e) {
            throw Exceptions.uncheck(e);
        }
    }

    private static final Set<String>  SYSTEM_PACKAGES    = new HashSet<>();
    private static final Set<String>  FRAMEWORK_PACKAGES = new HashSet<>();
    private static final Set<String>  FRAMEWORK_CLASSES  = new HashSet<>();
    static {
        SYSTEM_PACKAGES.add("java");
        SYSTEM_PACKAGES.add("sun");
        SYSTEM_PACKAGES.add("org.junit.");

        FRAMEWORK_PACKAGES.add("leap.junit.");
        FRAMEWORK_PACKAGES.add("leap.lang.");
        FRAMEWORK_PACKAGES.add("leap.core.");
    }

    protected boolean isIgnore(String name) {
        for(String p : SYSTEM_PACKAGES) {
            if(name.startsWith(p)) {
                return true;
            }
        }

        for(String p : FRAMEWORK_PACKAGES) {
            if(name.startsWith(p)) {
                return true;
            }
        }

        if(FRAMEWORK_CLASSES.contains(name)) {
            return true;
        }

        if(!useParent) {
            return false;
        }

        if(isBeanClass(name)) {
            return false;
        }

        if(name.startsWith(basePackage)) {
            return false;
        }

        return true;
    }
}