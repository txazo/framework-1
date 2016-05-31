/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package leap.core;

import leap.core.ds.DataSourceConfig;
import leap.core.ds.DataSourceManager;
import leap.core.instrument.AppInstrumentation;
import leap.core.sys.SysPermissionDef;
import leap.lang.*;
import leap.lang.accessor.MapPropertyAccessor;
import leap.lang.accessor.SystemPropertyAccessor;
import leap.lang.beans.BeanProperty;
import leap.lang.beans.BeanType;
import leap.lang.convert.Converts;
import leap.lang.logging.Log;
import leap.lang.logging.LogFactory;
import leap.lang.reflect.Reflection;
import leap.lang.resource.Resource;
import leap.lang.resource.ResourceSet;
import leap.lang.resource.Resources;
import leap.lang.resource.SimpleResourceSet;
import leap.lang.text.DefaultPlaceholderResolver;
import leap.lang.text.PlaceholderResolver;
import leap.lang.tools.DEV;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static leap.core.AppConfig.*;

public class DefaultAppConfigSource implements AppConfigSource {

	private static final Log log = LogFactory.get(DefaultAppConfigSource.class);

    protected static final String APP_PROFILE_CONFIG_RESOURCE       = "classpath:/profile";
    protected static final String APP_PROFILE_LOCAL_CONFIG_RESOURCE = "classpath:/profile_local";

    //all init properties
    protected static Set<String> INIT_PROPERTIES = new HashSet<>();
    static {
        INIT_PROPERTIES.add(AppConfig.INIT_PROPERTY_PROFILE);
        INIT_PROPERTIES.add(AppConfig.INIT_PROPERTY_BASE_PACKAGE);
        INIT_PROPERTIES.add(AppConfig.INIT_PROPERTY_DEBUG);
        INIT_PROPERTIES.add(AppConfig.INIT_PROPERTY_DEFAULT_CHARSET);
        INIT_PROPERTIES.add(AppConfig.INIT_PROPERTY_DEFAULT_LOCALE);
    }

    private static final List<AppPropertyReader> propertyReaders = Factory.newInstances(AppPropertyReader.class);
    private static final List<AppConfigReader>   configReaders   = Factory.newInstances(AppConfigReader.class);

    protected AppPropertyProcessor propertyProcessor = new PropertyProcessorWrapper();
    protected AppInstrumentation   instrumentation   = Factory.getInstance(AppInstrumentation.class);

    @Override
    public AppConfig loadConfiguration(Object externalContext, Map<String, String> initProperties) {
        if(null == initProperties) {
            initProperties = new LinkedHashMap<>();
        }

        //load init properties from system environment.
        loadInitPropertiesFromSystem(externalContext, initProperties);

        //init profile
        String profile = initProfile(externalContext, initProperties);

        //Create loader for loading the configuration.
        Loader loader = createLoader(externalContext, initProperties, profile);

        //Load config
        DefaultAppConfig config = loader.load();

        //post loading.
        postLoad(config);

        //instrument
        instrumentClasses(config);

        return config;
    }

    protected void postLoad(DefaultAppConfig config) {
        //load datasource form properties
        new DataSourceConfigPropertiesLoader(config.properties, config.dataSourceConfigs).load();

        config.postLoad();
    }

    protected void instrumentClasses(DefaultAppConfig config) {
        instrumentation.init(config);
        instrumentation.instrument(config.resources);
    }

    protected void loadInitPropertiesFromSystem(Object externalContext, Map<String, String> initProperties) {
        if(!initProperties.isEmpty()) {
            Maps.resolveValues(initProperties, new DefaultPlaceholderResolver(SystemPropertyAccessor.INSTANCE));
        }

        Properties props = System.getProperties();
        for(Object key : props.keySet()) {
            String name = key.toString();
            initProperties.put(name, System.getProperty(name));
        }

        for(String p : INIT_PROPERTIES){
            if(!initProperties.containsKey(p)){
                String v = System.getProperty(p);
                if(!Strings.isEmpty(v)){
                    initProperties.put(p, v);
                }
            }
        }
    }

    protected String initProfile(Object externalContext, Map<String, String> initProperties){
        String profile = initProperties.get(AppConfig.SYS_PROPERTY_PROFILE);
        if(!Strings.isEmpty(profile)) {
            return profile;
        }

        //read from init properties
        if(Strings.isEmpty(profile)){
            profile = initProperties.get(AppConfig.INIT_PROPERTY_PROFILE);
        }

        //Read local profile file if running in dev project.
        if(Strings.isEmpty(profile) && DEV.isDevProject(externalContext)) {
            //read from local profile file
            profile = readProfileFile(initProperties, APP_PROFILE_LOCAL_CONFIG_RESOURCE);
        }

        if(Strings.isEmpty(profile)) {
            //read from default profile file
            profile = readProfileFile(initProperties, APP_PROFILE_CONFIG_RESOURCE);
        }

        //auto detect profile name
        if(Strings.isEmpty(profile)){
            profile = autoDetectProfileName(externalContext);
        }

        if(Strings.isEmpty(profile)) {
            profile = AppConfig.PROFILE_PRODUCTION;
        }

        return profile;
    }

    protected String readProfileFile(Map<String,String> initProperties, String location) {
        Resource r = Resources.getResource(location);
        if(null != r && r.exists() && !r.isDirectory()){
            String profile = Strings.trim(r.getContent());
            if(profile.startsWith("${") && profile.endsWith("}")) {
                profile = initProperties.get(profile.substring(1,profile.length() - 1));
            }
            return profile;
        }
        return null;
    }

    protected String autoDetectProfileName(Object externalContext){
        //Auto detect development environment (maven environment)
        if(DEV.isDevProject(externalContext)){
            return AppConfig.PROFILE_DEVELOPMENT;
        }else{
            return null;
        }
    }

    protected Loader createLoader(Object externalContext, Map<String,String> initProperties, String profile) {
        return new Loader(externalContext, initProperties, profile);
    }

    protected void loadProperties(ConfigContext context, AppResource... resources) {
        for(AppResource ar : resources){
            try{
                Resource resource  = ar.getResource();
                String resourceUrl = resource.getURL().toString();

                if(log.isDebugEnabled()){
                    if(AppResources.isFrameworkResource(resourceUrl)) {
                        log.trace("Loading properties from : {}", resourceUrl);
                    }else{
                        log.debug("Loading properties from : {}", resourceUrl);
                    }
                }

                if(context.resources.contains(resourceUrl)){
                    throw new AppConfigException("Cycle importing detected of '" + resourceUrl + "', please check your config : " + resourceUrl);
                }

                context.resources.add(resourceUrl);

                context.setDefaultOverride(ar.isDefaultOverride());
                for(AppPropertyReader reader : propertyReaders) {
                    if(reader.readProperties(context, resource)) {
                        break;
                    }
                }
                context.resetDefaultOverride();
            }catch(IOException e) {
                throw new AppConfigException("I/O Exception",e);
            }
        }
    }

    private void loadConfig(ConfigContext context, AppResource... resources){
        for(AppResource ar : resources){
            try{
                Resource resource  = ar.getResource();
                String resourceUrl = resource.getURL().toString();

                if(log.isDebugEnabled()){
                    if(AppResources.isFrameworkResource(resourceUrl)) {
                        log.trace("Loading config from : {}", resourceUrl);
                    }else{
                        log.debug("Loading config from : {}", resourceUrl);
                    }
                }

                if(context.resources.contains(resourceUrl)){
                    throw new AppConfigException("Cycle importing detected of '" + resourceUrl + "', please check your config : " + resourceUrl);
                }

                context.resources.add(resourceUrl);

                context.setDefaultOverride(ar.isDefaultOverride());
                for(AppConfigReader reader : configReaders) {
                    if(reader.readFully(context, resource)) {
                        break;
                    }
                }
                context.resetDefaultOverride();

            }catch(IOException e) {
                throw new AppConfigException("I/O Exception",e);
            }
        }
    }

    protected class Loader {

        final DefaultAppConfigSource parent = DefaultAppConfigSource.this;

        final Object                     externalContext;
        final Map<String, String>        initProperties;
        final DefaultAppConfig           config;
        final AppResources               appResources;
        final AppResource[]              configResources;
        final Set<String>                resolvingProperties = new HashSet<>();
        final DefaultPlaceholderResolver resolver;

        protected final Set<String>                                  additionalPackages = new LinkedHashSet<>();
        protected final Map<String, String>                          properties         = new ConcurrentHashMap<>();
        protected final Map<String, List<String>>                    arrayProperties    = new ConcurrentHashMap<>();
        protected final Set<Resource>                                resources          = new HashSet<>();
        protected final List<SysPermissionDef>                       permissions        = new ArrayList<>();
        protected final Map<Class<?>, Map<String, SysPermissionDef>> typedPermissions   = new HashMap<>();
        protected final Map<String, DataSourceConfig.Builder>        dataSourceConfigs  = new HashMap<>();
        protected final Set<AppPropertyLoaderConfig>                 propertyLoaders;

        Loader(Object externalContext, Map<String,String> initProperties, String profile) {
            this.externalContext = externalContext;
            this.initProperties  = initProperties;
            this.config          = new DefaultAppConfig(profile);
            this.appResources    = AppResources.create(config);
            this.configResources = appResources.search("config");

            this.resolver = new DefaultPlaceholderResolver(this::resolveProperty);
            this.resolver.setEmptyUnresolvablePlaceholders(false);
            this.resolver.setIgnoreUnresolvablePlaceholders(true);

            this.propertyLoaders = new TreeSet<>(Comparators.ORDERED_COMPARATOR);
        }

        protected DefaultAppConfig load() {
            //load local properties
            loadLocalProperties(new ConfigContext(this, false, true));

            //load external properties.
            loadExternalProperties(new ConfigContext(this, false, true));

            //Load configuration.
            loadConfig(new ConfigContext(this, false, false));

            //complete loading configuration.
            complete();

            return config;
        }

        protected void loadLocalProperties(ConfigContext context) {
            parent.loadProperties(context, configResources);

            //external properties overrides the configured properties.
            properties.putAll(initProperties);

            Maps.accept(properties, AppConfig.INIT_PROPERTY_BASE_PACKAGE,    String.class,  (p) -> config.basePackage = p);
            Maps.accept(properties, AppConfig.INIT_PROPERTY_DEBUG,           Boolean.class, (d) -> config.debug = d);
            Maps.accept(properties, AppConfig.INIT_PROPERTY_DEFAULT_CHARSET, Charset.class, (c) -> config.defaultCharset = c);
            Maps.accept(properties, AppConfig.INIT_PROPERTY_DEFAULT_LOCALE,  Locale.class,  (l) -> config.defaultLocale = l);

            //base package
            if(Strings.isEmpty(config.basePackage)){
                config.basePackage = DEFAULT_BASE_PACKAGE;
                config.properties.put(INIT_PROPERTY_BASE_PACKAGE,config.basePackage);
            }

            //debug
            if(null == config.debug){
                config.debug = AppConfig.PROFILE_DEVELOPMENT.equals(config.getProfile()) ? true : false;
                config.properties.put(INIT_PROPERTY_DEBUG,String.valueOf(config.debug));
            }

            //default locale
            if(null == config.defaultLocale){
                config.defaultLocale = DEFAULT_LOCALE;
                config.properties.put(INIT_PROPERTY_DEFAULT_LOCALE,config.defaultLocale.toString());
            }

            //default charset
            if(null == config.defaultCharset){
                config.defaultCharset = DEFAULT_CHARSET;
                config.properties.put(INIT_PROPERTY_DEFAULT_CHARSET,config.defaultCharset.name());
            }

            log.info("{}:{}, {}:{}, {}:{}, {}:{}",
                    INIT_PROPERTY_PROFILE,config.profile,
                    INIT_PROPERTY_BASE_PACKAGE,config.basePackage,
                    INIT_PROPERTY_DEFAULT_LOCALE,config.defaultLocale.toString(),
                    INIT_PROPERTY_DEFAULT_CHARSET,config.defaultCharset.name());

            resolveProperties();
            processProperties();
        }

        protected void loadExternalProperties(ConfigContext context) {

            for(AppPropertyLoaderConfig conf : propertyLoaders) {

                if(!conf.load(this.properties)) {
                    log.info("Property loader '{}' disabled", conf.getClassName());
                    continue;
                }

                Class<?> cls = Classes.tryForName(conf.getClassName());
                if(null == cls) {
                    throw new AppConfigException("The property loader class '" + conf.getClassName() +
                                                 "' not found, check your config");
                }

                if(!AppPropertyLoader.class.isAssignableFrom(cls)) {
                    throw new AppConfigException("The loader class '" + conf.getClassName() +
                                                 "' must implements interface '" + AppPropertyLoader.class.getName() + "'");
                }

                AppPropertyLoader loader = (AppPropertyLoader) Reflection.newInstance(cls);

                BeanType bt = BeanType.of(cls);

                for(Map.Entry<String,String> prop : conf.getProperties().entrySet()) {
                    String name  = prop.getKey();
                    String value = resolver.resolveString(prop.getValue());

                    if(!Strings.isEmpty(value)) {
                        BeanProperty bp = bt.getProperty(name);
                        bp.setValue(loader, Converts.convert(value, bp.getType(), bp.getGenericType()));
                    }
                }

                log.info("Load properties by loader : {}", cls.getSimpleName());
                loader.loadProperties(context);
            }

            resolveProperties();
            processProperties();
        }

        protected void loadConfig(ConfigContext context) {
            AppResource[] files = appResources.search("config");
            if(files.length > 0) {
                parent.loadConfig(context, files);
            }
        }

        protected void complete() {
            //Apply all the properties to config object.
            config.loadProperties(this.properties);
            config.loadArrayProperties(this.arrayProperties);

            //resources
            try {
                Map<String,Resource> urlResourceMap = new HashMap<>();

                loadBasePackageResources(urlResourceMap,config.basePackage);
                loadResources(urlResourceMap);

                config.resources = new SimpleResourceSet(urlResourceMap.values().toArray(new Resource[]{}));
            } catch (IOException e) {
                throw new AppConfigException("Unexpected IOException : " + e.getMessage(), e);
            }

            //permissions
            config.permissions.addAll(permissions);

            log.info("Load {} properties",config.properties.size());
        }

        protected void processProperties() {
            Out<String> out = new Out<>();
            for(Map.Entry<String, String> p : this.properties.entrySet()) {
                String name  = p.getKey();
                String value = p.getValue();

                if(propertyProcessor.process(name, value, out)) {
                    value = out.getValue();
                    this.properties.put(name, value);
                }
            }

            for(Map.Entry<String,List<String>> p : this.arrayProperties.entrySet()) {
                String       name   = p.getKey();
                List<String> values = p.getValue();

                boolean processed = false;

                for(int i=0;i<values.size();i++) {
                    String value = values.get(i);

                    if(propertyProcessor.process(name, value, out)) {
                        value = out.getValue();
                        processed = true;
                        values.set(i, value);
                    }
                }

                if(processed) {
                    arrayProperties.put(name, values);
                }
            }

        }

        protected String resolveProperty(String name) {
            if(resolvingProperties.contains(name)) {
                throw new AppConfigException("Found cyclic reference property '" + name + "'");
            }

            resolvingProperties.add(name);

            String value = properties.get(name);

            if(resolver.hasPlaceholder(value)) {
                String newValue = resolver.resolveString(value);
                if(!newValue.equals(value)) {
                    properties.put(name,newValue);
                    value = newValue;
                }
            }else if(null == value) {
                value = initProperties.get(name);
            }

            resolvingProperties.remove(name);

            return value;
        }

        protected void resolveProperties() {
            resolveProperties(this.properties);

            for(Map.Entry<String,List<String>> p : arrayProperties.entrySet()) {
                String name = p.getKey();
                List<String> values = p.getValue();

                for(int i=0;i<values.size();i++) {
                    String value = values.get(i);
                    if(resolver.hasPlaceholder(value)) {
                        String newValue = resolver.resolveString(value);
                        if(!newValue.equals(value)) {
                            values.set(i, newValue);
                        }
                    }
                }
            }

            for(Map.Entry<String,DataSourceConfig.Builder> entry : dataSourceConfigs.entrySet()) {
                String name = entry.getKey();
                DataSourceConfig.Builder c = entry.getValue();

                Map<String, String> resolvedProperties = new ConcurrentHashMap<>(c.getProperties());

                resolveProperties(resolvedProperties);

                c.setProperties(resolvedProperties);

                config.dataSourceConfigs.put(name, c.build());
            }
        }

        protected void resolveProperties(Map<String, String> properties) {
            for(Map.Entry<String,String> p : properties.entrySet()) {
                String name  = p.getKey();
                String value = p.getValue();

                if(resolver.hasPlaceholder(value)) {
                    String newValue = resolver.resolveString(value);
                    if (!newValue.equals(value)) {
                        properties.put(name, newValue);
                    }
                }
            }
        }

        protected void loadResources(Map<String,Resource> urlResourceMap) throws IOException{
            for(String basePackage : additionalPackages) {
                loadBasePackageResources(urlResourceMap, basePackage);
            }
            for(Resource resource : resources){
                urlResourceMap.put(resource.getURL().toExternalForm(), resource);
            }
        }

        protected void loadBasePackageResources(Map<String, Resource> urlResourceMap, String basePackage) throws IOException {
            if(!Strings.isEmpty(basePackage)){
                ResourceSet rs = Resources.scanPackage(basePackage);

                log.debug("Scan {} resource(s) in base-package location '{}'",rs.size(),basePackage);

                for(Resource resource : rs){
                    urlResourceMap.put(resource.getURL().toExternalForm(), resource);
                }
            }
        }

        protected void addPermissions(List<SysPermissionDef> permissions, boolean override){
            for(SysPermissionDef permission : permissions){
                addPermission(permission, override);
            }
        }

        protected void addPermission(SysPermissionDef permission, boolean override){
            Map<String,SysPermissionDef> typesPermissionsMap = typedPermissions.get(permission.getPermType());

            SysPermissionDef exists = null;

            if(null == typesPermissionsMap){
                typesPermissionsMap = new HashMap<>();
            }else{
                exists = typesPermissionsMap.get(permission.getPermObject().getName());
            }

            if(!override && null != exists){
                throw new AppConfigException("Found duplicated permission '" + permission.toString() + "', source : " + permission.getSource() + "," + exists.getSource());
            }

            if(null != exists){
                permissions.remove(exists);
            }

            typesPermissionsMap.put(permission.getPermObject().getName(), permission);
            permissions.add(permission);
        }
    }

    protected class ConfigContext extends MapPropertyAccessor implements AppConfigContext,AppPropertyContext,AppPropertySetter {

        protected final boolean          forProperty;
        protected final Loader           loader;
        protected final DefaultAppConfig config;
        protected final boolean          originalDefaultOverride;

        protected boolean     defaultOverride;
        protected boolean     hasDefaultDataSource = false;
        protected Set<String> resources            = new HashSet<>();

        ConfigContext(Loader loader, boolean defaultOverride, boolean forProperty){
            super(loader.properties);
            this.loader = loader;
            this.config = loader.config;
            this.originalDefaultOverride = defaultOverride;
            this.defaultOverride = defaultOverride;
            this.forProperty = forProperty;
        }

        @Override
        public String getProfile() {
            return config.profile;
        }

        @Override
        public boolean isDefaultOverride() {
            return defaultOverride;
        }

        @Override
        public void setDefaultOverride(boolean b) {
            this.defaultOverride = b;
        }

        @Override
        public void resetDefaultOverride() {
            this.defaultOverride = originalDefaultOverride;
        }

        @Override
        public Boolean getDebug() {
            return config.debug;
        }

        @Override
        public void setDebug(boolean debug) {
            config.debug = debug;
        }

        @Override
        public String getBasePackage() {
            return config.basePackage;
        }

        @Override
        public void setBasePackage(String bp) {
            config.basePackage = bp;
        }

        @Override
        public Locale getDefaultLocale() {
            return config.defaultLocale;
        }

        @Override
        public void setDefaultLocale(Locale locale) {
            config.defaultLocale = locale;
        }

        @Override
        public Charset getDefaultCharset() {
            return config.defaultCharset;
        }

        @Override
        public void setDefaultCharset(Charset charset) {
            config.defaultCharset = charset;
        }

        @Override
        public Set<String> getAdditionalPackages() {
            return loader.additionalPackages;
        }

        @Override
        public Map<String, String> getProperties() {
            return properties;
        }

        @Override
        public Map<String, List<String>> getArrayProperties() {
            return loader.arrayProperties;
        }

        @Override
        public boolean hasArrayProperty(String name) {
            return loader.arrayProperties.containsKey(name);
        }

        @Override
        public List<SysPermissionDef> getPermissions() {
            return loader.permissions;
        }

        @Override
        public void addPermission(SysPermissionDef p, boolean override) {
            loader.addPermission(p, override);
        }

        @Override
        public void importResource(Resource resource, boolean override) {
            AppResource ar = new SimpleAppResource(resource, override);

            if(forProperty) {
                loadProperties(new ConfigContext(loader, override, true),  ar);
            }else{
                loadConfig(new ConfigContext(loader, override, false), ar);
            }
        }

        @Override
        public void putProperties(Map<String, String> props) {
            if(null != props) {
                props.forEach((k,v) -> putProperty(null, k, v));
            }
        }

        @Override
        public void putProperty(Object source, String name, String value) {
            if(name.endsWith("[]")) {
                name = name.substring(0, name.length()-2);
                List<String> list = loader.arrayProperties.get(name);
                if(null == list) {
                    list = new ArrayList<>();
                    loader.arrayProperties.put(name, list);
                }
                list.add(value);
            }else{
                loader.properties.put(name, value);
            }
        }

        @Override
        public void putProperties(Object source, Map<String, String> props) {
            this.putProperties(props);
        }

        @Override
        public void addLoader(AppPropertyLoaderConfig config) {
            Args.notNull(config);
            loader.propertyLoaders.add(config);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getExtension(Class<T> type) {
            return (T)config.extensions.get(type);
        }

        @Override
        public <T> void setExtension(T extension) {
            Args.notNull(extension);
            config.extensions.put(extension.getClass(), extension);
        }

        @Override
        public <T> void setExtension(Class<T> type, T extension) {
            Args.notNull(type);
            Args.notNull(extension);
            config.extensions.put(type, extension);
        }

        @Override
        public void addResource(Resource r) {
            if(null != r) {
                loader.resources.add(r);
            }
        }

        @Override
        public void addResources(ResourceSet rs) {
            if(null != rs) {
                for(Resource r : rs.toResourceArray()) {
                    loader.resources.add(r);
                }
            }
        }

        @Override
        public boolean hasDefaultDataSourceConfig() {
            return hasDefaultDataSource;
        }

        @Override
        public boolean hasDataSourceConfig(String name) {
            return loader.dataSourceConfigs.containsKey(name);
        }

        @Override
        public void setDataSourceConfig(String name, DataSourceConfig.Builder conf) {
            loader.dataSourceConfigs.put(name, conf);

            if(conf.isDefault()) {

                if (hasDefaultDataSource) {
                    throw new AppConfigException("Default DataSource already exists");
                }

                this.hasDefaultDataSource = true;
            }
        }

        @Override
        public PlaceholderResolver getPlaceholderResolver() {
            return loader.resolver;
        }
    }

    protected static class PropertyProcessorWrapper implements AppPropertyProcessor {

        private final AppPropertyProcessor[] processors =
                Factory.newInstances(AppPropertyProcessor.class).toArray(new AppPropertyProcessor[]{});

        @Override
        public boolean process(String name, String value, Out<String> newValue) {
            if(processors.length > 0) {
                for(int i=0;i<processors.length;i++) {
                    AppPropertyProcessor p = processors[i];
                    if(p.process(name, value, newValue)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    protected static class DataSourceConfigPropertiesLoader {
        protected static final String DB_DEFAULT_PREFIX = "db.";
        protected static final String DB_NAMED_PREFIX   = "db_";

        protected final Map<String, String>           properties;
        protected final Map<String, DataSourceConfig> dataSourceConfigs;

        public DataSourceConfigPropertiesLoader(Map<String, String> properties, Map<String, DataSourceConfig> dataSourceConfigs) {
            this.properties = properties;
            this.dataSourceConfigs = dataSourceConfigs;
        }

        protected void load() {
            Map<String, DataSourceConfig.Builder> dsMap = new HashMap<>();

            for(Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();

                if(key.startsWith(DB_DEFAULT_PREFIX)) {
                    if(dataSourceConfigs.containsKey(DataSourceManager.DEFAULT_DATASOURCE_NAME)) {
                        throw new AppConfigException("DataSource '" + DataSourceManager.DEFAULT_DATASOURCE_NAME + "' already configured, check property '" + key + "'");
                    }

                    DataSourceConfig.Builder conf = dsMap.get(DataSourceManager.DEFAULT_DATASOURCE_NAME);
                    if(null == conf) {
                        conf = new DataSourceConfig.Builder();
                        conf.setDefault(true);
                        dsMap.put(DataSourceManager.DEFAULT_DATASOURCE_NAME, conf);
                    }

                    conf.setProperty(key.substring(DB_DEFAULT_PREFIX.length()), val);
                    continue;
                }

                if(key.startsWith(DB_NAMED_PREFIX)) {
                    int dotIndex = key.indexOf(DB_NAMED_PREFIX.length(), '.');
                    if(dotIndex > 0) {

                        String dataSourceName = key.substring(DB_NAMED_PREFIX.length(), dotIndex);
                        String dataSourceProp = key.substring(dotIndex + 1);

                        if(dataSourceConfigs.containsKey(dataSourceName)) {
                            throw new AppConfigException("DataSource '" + dataSourceName + "' already configured, check property '" + key + "'");
                        }

                        DataSourceConfig.Builder conf = dsMap.get(dataSourceName);
                        if(null == conf) {
                            conf = new DataSourceConfig.Builder();
                            dsMap.put(dataSourceName, conf);
                        }

                        conf.setProperty(dataSourceProp, val);
                        continue;
                    }
                }
            }

            if(!dsMap.isEmpty()) {
                for(Map.Entry<String, DataSourceConfig.Builder> entry : dsMap.entrySet()) {
                    DataSourceConfig c = entry.getValue().build();

                    if(c.isValid()) {
                        dataSourceConfigs.put(entry.getKey(), c);
                    }
                }
            }
        }
    }

}