/*
 * Copyright 2014 the original author or authors.
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
package app.controller;

import tests.HtplTestCase;
import leap.lang.http.MimeTypes;
import leap.webunit.client.THttpResponse;

import org.junit.Test;

public class ResourceViewTest extends HtplTestCase {

	@Test
	public void testJsView() {
		THttpResponse resp = get("/test.js?$debug=0").assertContentEquals("var i=100;");
		assertEquals(MimeTypes.TEXT_JAVASCRIPT_TYPE.getSubtype(),resp.getContentType().getSubtype());
	}
	
	@Test
	public void testCssView() {
		THttpResponse resp = get("/test.css?$debug=0").assertContentEquals("#header{color:red;}");
		assertEquals(MimeTypes.TEXT_CSS_TYPE.getSubtype(),resp.getContentType().getSubtype());
	}
	
}