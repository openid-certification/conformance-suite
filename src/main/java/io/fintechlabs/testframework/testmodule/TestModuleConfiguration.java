/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.testmodule;

import java.util.Iterator;

import com.google.common.base.Splitter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A container class for test module configurations, with easy accessor methods.
 * 
 * @author jricher
 *
 */
public class TestModuleConfiguration {

	private JsonObject config;
	
	/**
	 * @param config
	 */
	public TestModuleConfiguration(JsonObject config) {
		this.config = config;
	}

	/**
	 * Search the config file for a key with lookup values separated by ".", so "foo.bar.baz" is found in:
	 * 
	 * {
	 *  foo: { 
	 *    bar: { 
	 *      baz: "value" 
	 *    } 
	 *  } 
	 * }
	 *  
	 * @param key
	 */
	public String getString(String key) {
		JsonElement e = findElement(key);

		if (e != null) {
			if (e.isJsonPrimitive()) {
				return e.getAsString();
			} else {
				// it wasn't a primitive
				return null;
			}
		} else {
			// we didn't find it
			return null;
		}
	}
	
	
	public JsonElement findElement(String key) {
	
		//
		// TODO: memoize this lookup for efficiency
		// 
		
		// start at the top
		JsonElement e = config;
		
		Iterable<String> parts = Splitter.on('.').split(key);
		Iterator<String> it = parts.iterator();

		while (it.hasNext()) {
			String p = it.next();
			if (e.isJsonObject()) {
				JsonObject o = e.getAsJsonObject();
				if (o.has(p)) {
					e = o.get(p); // found the next level
					if (!it.hasNext()) {
						// we've reached a leaf at the right part of the key, return what we found
						return e;
					}
				} else {
					// didn't find it, stop processing
					break;
				}
			} else {
				// didn't find it, stop processing
				break;
			}
		}

		// didn't find it
		return null;
		
	}


}
