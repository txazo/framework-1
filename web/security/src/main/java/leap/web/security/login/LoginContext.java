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
package leap.web.security.login;

import leap.core.security.Credentials;
import leap.core.security.UserPrincipal;
import leap.web.Request;
import leap.web.Response;
import leap.web.security.authc.AuthenticationContext;

public interface LoginContext extends AuthenticationContext {
	
	String getReturnUrl();
	
	void setReturnUrl(String returnUrl);
	
	String getLoginUrl();
	
	void setLoginUrl(String loginUrl);
	
	boolean isCredentialsResolved();

	Credentials getCredentials();
	
	void setCredentials(Credentials credentials);
	
	boolean isAuthenticated();
	
	UserPrincipal getUser();
	
	void setUser(UserPrincipal principal);
}