/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package app;

import app.models.User;
import leap.core.security.SEC;
import leap.web.App;

public class Global extends App {

    @Override
    protected void init() throws Throwable {
        User admin = new User();
        admin.setName("Admin");
        admin.setLoginName("admin");
        admin.setPassword(SEC.encodePassword("1"));
        admin.setEnabled(true);
        admin.create();
    }
}
