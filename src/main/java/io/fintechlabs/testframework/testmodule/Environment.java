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

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

/**
 * An element for storing the current running state of a test module in a way that it can be passed around.
 * 
 * @author jricher
 *
 */
public class Environment {

	private Map<String, JsonObject> store = new HashMap<>();

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(String key) {
		return store.containsKey(key);
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public JsonObject get(String key) {
		return store.get(key);
	}

	/**
	 * Look up a single-string entry
	 * @param key
	 * @return
	 */
	public String getString(String key) {
		JsonObject o = get(key);
		if (o != null) {
			if (o.has("val")) {
				if (o.get("val").isJsonPrimitive()) {
					return o.get("val").getAsString();
				}
			}
		}
		
		// otherwise, return null
		return null;
	}
	
	/**
	 * @param key
	 * @param value
	 * @return
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public JsonObject put(String key, JsonObject value) {
		return store.put(key, value);
	}
	
	/**
	 * Store a single string as a value
	 * @param key
	 * @param value
	 * @return
	 */
	public JsonObject put(String key, String value) {
		JsonObject o = new JsonObject();
		o.addProperty("val", value);
		return put(key, o);
	}
	
	
}
