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
package leap.oauth2.server.sso;

import leap.oauth2.server.authc.AuthzAuthentication;
import leap.web.Request;
import leap.web.Response;
import leap.web.security.logout.LogoutContext;

public interface AuthzSSOManager {

    void setSSOSession(AuthzSSOSession session, Request request, AuthzAuthentication authc) throws Throwable;

    void setCurrentSSOLogin(AuthzSSOLogin login, Request request, AuthzAuthentication authc) throws Throwable;
    
    AuthzSSOSession getSSOSession(Request request, Response response, AuthzAuthentication authc) throws Throwable;

    AuthzSSOLogin getCurrentSSOLogin(Request request, Response response, AuthzAuthentication authc) throws Throwable;
    
    void onOAuth2LoginSuccess(Request request, Response response, AuthzAuthentication authc) throws Throwable;

    String[] resolveLogoutUrls(Request request, Response response, LogoutContext context) throws Throwable;
}