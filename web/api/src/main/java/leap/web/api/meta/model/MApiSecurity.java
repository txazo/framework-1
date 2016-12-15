/*
 *
 *  * Copyright 2013 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package leap.web.api.meta.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by kael on 2016/12/8.
 */
public class MApiSecurity extends MApiNamed {

    private List<String> scopes = new ArrayList<>();

    public MApiSecurity(String name) {
        super(name);
    }

    public MApiSecurity(String name, String title) {
        super(name, title);
    }

    public MApiSecurity(String name, String title, Map<String, Object> attrs) {
        super(name, title, attrs);
    }

    public void addScopes(String ... scopes){
        if(scopes != null && scopes.length > 0){
            this.scopes.addAll(Arrays.asList(scopes));
        }
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
}
