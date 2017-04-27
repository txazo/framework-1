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

import java.util.List;

import leap.core.cache.Cache;
import leap.core.junit.AppTestBase;
import leap.lang.Lazy;

import org.junit.Test;

import test.beans.InjectBean;
import test.beans.PrimaryBean;
import test.beans.PrimaryBean1;
import test.beans.PrimaryBean2;
import test.beans.TestBean;

public class BeanFactoryTest extends AppTestBase {

	@Test
	public void testConstructor1(){
		TestBean bean = AppContext.factory().getBean("testConstructor1");
		assertEquals("hello", bean.getString());
	}
	
	@Test
	public void testPrimaryBean(){
		assertNotNull(factory.getBean(PrimaryBean.class));
		assertEquals("1",factory.getBean(PrimaryBean1.class).getValue());
		assertEquals(factory.getBean(PrimaryBean2.class).getValue(),"2");
		
	}
	
	@Test
	public void testProfile(){
		assertNull(AppContext.factory().tryGetBean("testProfile.shouldNotCreated"));
		assertNotNull(AppContext.factory().getBean("testProfile.shouldBeCreated"));
	}
	
	@Test
	public void testAliasBean() {
		Object bean      = factory.getBean(Cache.class,"test");
		Object aliasBean = factory.getBean(Cache.class,"testAlias");
		assertSame(bean, aliasBean);
	}

	@Test
	public void testLazy() {
		InjectBean bean = factory.getBean(InjectBean.class);
		Lazy<PrimaryBean> lazyPrimaryBean = bean.lazyPrimaryBean;
		assertNotNull(lazyPrimaryBean);
		assertSame(factory.getBean(PrimaryBean.class), lazyPrimaryBean.get());
		assertSame(lazyPrimaryBean.get(), lazyPrimaryBean.get());
		
		Lazy<List<PrimaryBean>> lazyPrimaryBeans = bean.lazyPrimaryBeans;
		assertNotNull(lazyPrimaryBeans);
		
		List<PrimaryBean> primaryBeans = lazyPrimaryBeans.get();
		assertNotNull(primaryBeans);
		assertEquals(1, primaryBeans.size());
	}
	
	@Test
	public void testInjectPrivateField() {
		InjectBean bean = factory.getBean(InjectBean.class);
		assertNotNull(bean.nonGetterGetPrivateInjectPrimaryBean());
		assertNull(bean.nonGetterGetNotInjectPrimaryBean());
	}
}