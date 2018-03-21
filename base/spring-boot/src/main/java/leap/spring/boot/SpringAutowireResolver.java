/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package leap.spring.boot;

import leap.lang.Objects2;
import leap.lang.logging.Log;
import leap.lang.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AutowireCandidateResolver;

import java.lang.reflect.Type;

final class SpringAutowireResolver implements AutowireCandidateResolver {

    private static final Log log = LogFactory.get(SpringAutowireResolver.class);

    private final AutowireCandidateResolver original;

    public SpringAutowireResolver(AutowireCandidateResolver original) {
        this.original = original;
    }

    @Override
    public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
        return null != original ? original.isAutowireCandidate(bdHolder, descriptor) : false;
    }

    @Override
    public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, String beanName) {
        return null != original ? original.getLazyResolutionProxyIfNecessary(descriptor, beanName) : null;
    }

    @Override
    public Object getSuggestedValue(DependencyDescriptor descriptor) {
        Object bean = null != original ? original.getSuggestedValue(descriptor) : null;

        if(null == bean && null != Global.leap) {
            Class type = descriptor.getDependencyType();

            if(!type.getName().startsWith(Global.SPRING_PACKAGE_PREFIX)) {

                Type genericType = null;

                if(null != descriptor.getField()) {
                    genericType = descriptor.getField().getGenericType();
                }else if(null != descriptor.getMethodParameter()) {
                    genericType = descriptor.getMethodParameter().getGenericParameterType();
                }

                try {
                    LeapBeanSupport.disable();

                    bean = Global.leap.factory().resolveInjectValue(type, genericType);
                    if (null != bean && Objects2.isEmpty(bean)) {
                        bean = null;
                    }
                    if (null != bean) {
                        log.debug("Found leap managed bean of type '{}'", type);
                    }else {
                        log.debug("No leap managed bean of type '{}'", type);
                    }
                }finally {
                    LeapBeanSupport.enable();
                }
            }
        }
        return bean;
    }
}