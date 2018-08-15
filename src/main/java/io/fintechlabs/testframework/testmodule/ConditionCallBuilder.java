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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;

/**
 * Utility class to collect the methods related to a Condition call.
 *
 * @author jricher
 *
 */
public class ConditionCallBuilder {

	private Class<? extends Condition> conditionClass = null;
	private List<String> requirements = new ArrayList<>();
	private ConditionResult onFail = ConditionResult.FAILURE;
	private ConditionResult onSkip = ConditionResult.INFO;
	private boolean stopOnFailure = true;
	private List<String> skipIfObjectsRequired = new ArrayList<>();
	private List<String> skipIfStringsRequired = new ArrayList<>();
	private List<Pair<String, String>> skipIfElementsRequired = new ArrayList<>();

	private Map<String, String> mapKeys = new HashMap<>();
	private List<String> unmapKeys = new ArrayList<>();

	public ConditionCallBuilder(Class<? extends Condition> conditionClass) {
		this.conditionClass = conditionClass;
	}

	public ConditionCallBuilder requirement(String requirement) {
		this.requirements.add(requirement);
		return this;
	}

	/**
	 * @param requirements the requirements to set
	 */
	public ConditionCallBuilder requirements(String... requirements) {
		Collections.addAll(this.requirements, requirements);
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

	public ConditionCallBuilder skipIfObjectRequired(String skipIfRequired) {
		this.skipIfObjectsRequired.add(skipIfRequired);
		return this;
	}

	/**
	 * @param skipIfObjectsRequired the skipIfObjectsRequired to set
	 */
	public ConditionCallBuilder skipIfObjectsRequired(String... skipIfRequired) {
		if (skipIfRequired != null) {
			Collections.addAll(this.skipIfObjectsRequired, skipIfRequired);
		}
		return this;
	}

	public ConditionCallBuilder skipIfStringRequired(String skipIfStringRequired) {
		this.skipIfStringsRequired.add(skipIfStringRequired);
		return this;
	}

	/**
	 * @param skipIfStringsRequired the skipIfStringsRequired to set
	 */
	public ConditionCallBuilder skipIfStringsRequired(String... skipIfStringsRequired) {
		if (skipIfStringsRequired != null) {
			Collections.addAll(this.skipIfStringsRequired, skipIfStringsRequired);
		}
		return this;
	}

	public ConditionCallBuilder skipIfElementRequired(String objId, String path) {
		if (objId != null && path != null) {
			this.skipIfElementsRequired.add(new ImmutablePair<>(objId, path));
		}
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
	public List<String> getRequirements() {
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
	 * @return the skipIfObjectsRequired
	 */
	public List<String> getSkipIfObjectsRequired() {
		return skipIfObjectsRequired;
	}

	/**
	 * @return the skipIfStringsRequired
	 */
	public List<String> getSkipIfStringsRequired() {
		return skipIfStringsRequired;
	}

	public List<Pair<String,String>> getSkipIfElementsRequired() {
		return skipIfElementsRequired;
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
