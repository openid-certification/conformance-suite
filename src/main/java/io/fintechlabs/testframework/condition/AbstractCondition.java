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

package io.fintechlabs.testframework.condition;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public abstract class AbstractCondition implements Condition {
	
	private String testId;
	private EventLog log;
	private Set<String> requirements;
	
	/**
	 * @param testId
	 * @param log
	 */
	protected AbstractCondition(String testId, EventLog log) {
		this(testId, log, Collections.emptySet());
	}
	
	protected AbstractCondition(String testId, EventLog log, String... requirements) {
		this(testId, log, Sets.newHashSet(requirements));
	}
	
	protected AbstractCondition(String testId, EventLog log, Set<String> requirements) {
		this.testId = testId;
		this.log = log;
		this.requirements = requirements;
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#getMessage()
	 */
	public String getMessage() {
		return this.getClass().getSimpleName();
	}

	/*
	 * Logging utilities
	 */
	
	protected void log(JsonObject obj) {
		log.log(testId, getMessage(), obj);
	}
	
	protected void log(String msg) {
		log.log(testId, getMessage(), msg);
	}
	
	protected void log(Map<String, Object> map) {
		log.log(testId, getMessage(), map);
	}
	
	protected void log(String msg, JsonObject obj) {
		obj.addProperty("msg", msg);
		log(obj);
	}

	protected void log(String msg, Map<String, Object> map) {
		Map<String, Object> copy = new HashMap<>(map);
		copy.put("msg", msg);
		log(map);
	}
	
	protected void logSuccess() {
		if (getRequirements().isEmpty()) {
			log(ImmutableMap.of("result", "SUCCESS"));
		} else {
			log(ImmutableMap.of("result", "SUCCESS", "requirements", getRequirements()));
		}
	}

	protected void logFailure() {
		if (getRequirements().isEmpty()) {
			log(ImmutableMap.of("result", "FAILURE"));
		} else {
			log(ImmutableMap.of("result", "FAILURE", "requirements", getRequirements()));
		}
	}
	
	protected void logFailure(String msg) {
		if (getRequirements().isEmpty()) {
			log(msg, ImmutableMap.of("result", "FAILURE"));
		} else {
			log(msg, ImmutableMap.of("result", "FAILURE", "requirements", getRequirements()));
		}
	}

	/*
	 * Error utilities
	 */

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(String message, Throwable cause) {
		logFailure(message);
		throw new ConditionError(testId, getMessage() + ": " + message, cause);
	}

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(String message) {
		logFailure(message);
		throw new ConditionError(testId, getMessage() + ": " + message);
	}

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(Throwable cause) {
		logFailure();
		throw new ConditionError(testId, getMessage(), cause);
	}
	
	
	/**
	 * Get the list of requirements that this test would fulfill if it passed
	 * @return
	 */
	protected Set<String> getRequirements() {
		return requirements;
	}
	
	protected void createUploadPlaceholder(String msg) {
		if (getRequirements().isEmpty()) {
			log(msg, ImmutableMap.of("upload", RandomStringUtils.randomAlphanumeric(10), "result", "REVIEW"));
		} else {
			log(msg, ImmutableMap.of("upload", RandomStringUtils.randomAlphanumeric(10), "result", "REVIEW", "requirements", getRequirements()));
		}
	}
	
	protected void createUploadPlaceholder() {
		if (getRequirements().isEmpty()) {
			log(ImmutableMap.of("upload", RandomStringUtils.randomAlphanumeric(10), "result", "REVIEW"));
		} else {
			log(ImmutableMap.of("upload", RandomStringUtils.randomAlphanumeric(10), "result", "REVIEW", "requirements", getRequirements()));
		}
	}
}
