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
package leap.core.el.function;

import leap.lang.Arrays2;
import leap.lang.el.ElException;

public class ArrayFuncs {

	public static boolean contains(Object array, Object item) {
		if(null == array || null == item){
			return false;
		}
		
		if(array instanceof Object[]) {
			return Arrays2.contains((Object[])array,item);
		}
		
		if(array.getClass().isArray()) {
			return Arrays2.containsInObjectArray(array, item);
		}
		
		throw new ElException("Invalid array object '" + array + "'");
	}
	
	protected ArrayFuncs() {
		
	}
}
