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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;

/**
 * Utility class to collect the methods related to a Condition call.
 *
 * @author jricher
 *
 */
public class ConditionCallBuilder {

	private static final String[] EMPTY_ARRAY = new String[0];

	private Class<? extends Condition> conditionClass = null;
	private String[] requirements = EMPTY_ARRAY;
	private ConditionResult onFail = ConditionResult.FAILURE;
	private ConditionResult onSkip = ConditionResult.INFO;
	private boolean stopOnFailure = true;
	private String[] skipIfRequired = EMPTY_ARRAY;
	private String[] skipIfStringsRequired = EMPTY_ARRAY;

	private Map<String, String> mapKeys = new HashMap<>();
	private List<String> unmapKeys = new ArrayList<>();

	public ConditionCallBuilder(Class<? extends Condition> conditionClass) {
		this.conditionClass = conditionClass;
	}

	/**
	 * @param requirements the requirements to set
	 */
	public ConditionCallBuilder requirements(String... requirements) {
		this.requirements = requirements;
		return this;
	}

	/**
	 * @param onFail the onFail to set
	 */
	public ConditionCallBuilder onFail(ConditionResult onFail) {
		this.onFail = onFail;
		return this;
	}

	/**
	 * @param onSkip the onSkip to set
	 */
	public ConditionCallBuilder onSkip(ConditionResult onSkip) {
		this.onSkip = onSkip;
		return this;
	}

	/**
	 * @param stopOnFailure the stopOnFailure to set
	 */
	public ConditionCallBuilder dontStopOnFailure() {
		this.stopOnFailure = false;
		return this;
	}

	/**
	 * @param skipIfRequired the skipIfRequired to set
	 */
	public ConditionCallBuilder skipIfRequired(String... skipIfRequired) {
		this.skipIfRequired = skipIfRequired;
		return this;
	}

	/**
	 * @param skipIfStringsRequired the skipIfStringsRequired to set
	 */
	public ConditionCallBuilder skipIfStringsRequired(String... skipIfStringsRequired) {
		this.skipIfStringsRequired = skipIfStringsRequired;
		return this;
	}


	public ConditionCallBuilder mapKey(String from, String to) {
		mapKeys.put(from, to);
		return this;
	}

	public ConditionCallBuilder unmapKey(String key) {
		unmapKeys.add(key);
		return this;
	}

	// getters

	/**
	 * @return the conditionClass
	 */
	public Class<? extends Condition> getConditionClass() {
		return conditionClass;
	}

	/**
	 * @return the requirements
	 */
	public String[] getRequirements() {
		return requirements;
	}

	/**
	 * @return the onFail
	 */
	public ConditionResult getOnFail() {
		return onFail;
	}

	/**
	 * @return the onSkip
	 */
	public ConditionResult getOnSkip() {
		return onSkip;
	}

	/**
	 * @return the stopOnFailure
	 */
	public boolean isStopOnFailure() {
		return stopOnFailure;
	}

	/**
	 * @return the skipIfRequired
	 */
	public String[] getSkipIfRequired() {
		return skipIfRequired;
	}

	/**
	 * @return the skipIfStringsRequired
	 */
	public String[] getSkipIfStringsRequired() {
		return skipIfStringsRequired;
	}


	/**
	 * @return the mapKeys
	 */
	public Map<String, String> getMapKeys() {
		return mapKeys;
	}

	/**
	 * @return the unmapKeys
	 */
	public List<String> getUnmapKeys() {
		return unmapKeys;
	}


}
