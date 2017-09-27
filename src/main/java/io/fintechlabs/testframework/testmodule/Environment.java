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
import java.util.Map;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * An element for storing the current running state of a test module in a way that it can be passed around.
 * 
 * This object stores JSON indexed by strings. Furthermore, the JSON can be indexed by foo.bar.baz selectors. 
 * 
 * @author jricher
 *
 */
public class Environment {

	private static final String STRING_VALUES = "_STRING_VALUES";
	private Map<String, JsonObject> store = Maps.newHashMap(
			ImmutableMap.of(STRING_VALUES, new JsonObject())); // make sure we start with a place to put the string values

	/**
	 * Look to see if the JSON object is in this environment
	 * 
	 * @param objId
	 * @return
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsObj(String objId) {
		return store.containsKey(objId);
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
		return getString(STRING_VALUES, key);
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
	public JsonObject putString(String key, String value) {
		JsonObject o = get(STRING_VALUES);
		o.addProperty(key, value);
		return o;
	}

	/**
	 * Search the environment for a key with lookup values separated by ".", so "foo.bar.baz" is found in:
	 * 
	 * {
	 *  foo: { 
	 *    bar: { 
	 *      baz: "value" 
	 *    } 
	 *  } 
	 * }
	 *  
	 * @param path
	 */
	public String getString(String objId, String path) {
		JsonElement e = findElement(objId, path);

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
	
	
	public JsonElement findElement(String objId, String path) {
	
		//
		// TODO: memoize this lookup for efficiency
		// 
		
		// start at the top
		JsonElement e = get(objId);
		
		if (e == null) {
			return null;
		}
		
		Iterable<String> parts = Splitter.on('.').split(path);
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
