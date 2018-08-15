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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A builder class for test execution controls, such as mapping and unmapping keys in the environment,
 * starting and stopping blocks in the logs, and other controls not directly related to calling a
 * condition.
 *
 * @author jricher
 *
 */
public class TestExecutionBuilder {
	private Map<String, String> mapKeys = new LinkedHashMap<>();
	private List<String> unmapKeys = new ArrayList<>();

	/**
	 * Map a key in the environment. If this is called multiple times, the keys will be mapped in the same order they
	 * are recorded in this builder.
	 *
	 * See Environment.mapKey(String, String)
	 *
	 * @param from the key to map from (they key that will be called from the outside, any underlying object currently referenced by this key will be effectively hidden)
	 * @param to the key to map to (the key that references the underlying object that will be returned when "from" is used in the Environment)
	 * @return this builder
	 */
	public TestExecutionBuilder mapKey(String from, String to) {
		mapKeys.put(from, to);
		return this;
	}

	/**
	 * Remove a mapping on the given key. If this is called multiple times, the keys will be mapped in the same order they
	 * are recorded in this builder.
	 *
	 * See Environment.unmapKey(String)
	 *
	 * @param key the key to unmap; subsequent calls to use this key in the Environment will return the original underlying object
	 * @return this builder
	 */
	public TestExecutionBuilder unmapKey(String key) {
		unmapKeys.add(key);
		return this;
	}

	/**
	 * Get the set of keys to map in the Environment, in order of addition to this builder.
	 *
	 * @return A map of all keys to map in the environment as as "from -> to" sets.
	 */
	public Map<String, String> getMapKeys() {
		return mapKeys;
	}

	/**
	 * Get the list of keys to unmap in the Environment, in order of addition to this builder.
	 *
	 * @return A list of all keys to unmap.
	 */
	public List<String> getUnmapKeys() {
		return unmapKeys;
	}



}
