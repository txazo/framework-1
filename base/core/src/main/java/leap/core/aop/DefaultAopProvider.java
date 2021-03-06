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

package leap.core.aop;

import leap.core.AppContext;

public class DefaultAopProvider implements AopProvider {

    protected AopConfig config;

    public DefaultAopProvider() {
        AppContext context = AppContext.tryGetCurrent();
        if(null != context) {
            config = context.getConfig().getExtension(AopConfig.class);
        }
    }

    @Override
    public void run(MethodInterception interception) throws Throwable {
        if(null != config && config.isEnabled()) {
            interception.execute();
        }else{
            interception.executeRaw();
        }
    }

    @Override
    public <T> T runWithResult(MethodInterception interception) throws Throwable {
        if(null != config && config.isEnabled()) {
            return (T)interception.execute();
        }else{
            return (T)interception.executeRaw();
        }
    }

}
