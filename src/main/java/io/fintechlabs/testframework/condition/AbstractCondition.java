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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
	private boolean optional;
	
	/**
	 * @param testId
	 * @param log
	 */
	protected AbstractCondition(String testId, EventLog log, boolean optional) {
		this(testId, log, optional, Collections.emptySet());
	}
	
	protected AbstractCondition(String testId, EventLog log, boolean optional, String... requirements) {
		this(testId, log, optional, Sets.newHashSet(requirements));
	}
	
	protected AbstractCondition(String testId, EventLog log, boolean optional, Set<String> requirements) {
		this.testId = testId;
		this.log = log;
		this.optional = optional;
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
	
	protected void log(String msg, JsonObject in) {
		JsonObject obj = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		obj.addProperty("msg", msg);
		log(obj);
	}

	protected void log(String msg, Map<String, Object> map) {
		Map<String, Object> copy = new HashMap<>(map); // don't modify the underlying map
		copy.put("msg", msg);
		log(copy);
	}
	
	protected void logSuccess(JsonObject in) {
		JsonObject obj = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		obj.addProperty("result", "SUCCESS");
		log(obj);
	}
	
	protected void logSuccess(String msg) {
		if (getRequirements().isEmpty()) {
			log(args("msg", msg, "result", "SUCCESS"));
		} else {
			log(args("msg", msg, "result", "SUCCESS", "requirements", getRequirements()));
		}
	}
	
	protected void logSuccess(Map<String, Object> map) {
		Map<String, Object> copy = new HashMap<>(map); // don't modify the underlying map
		copy.put("result", "SUCCESS");
		if (!getRequirements().isEmpty()) {
			copy.put("requirements", getRequirements());
		}
		log(map);
	}
	
	protected void logSuccess(String msg, JsonObject in) {
		JsonObject obj = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		obj.addProperty("msg", msg);
		obj.addProperty("result", "SUCCESS");
		if (!getRequirements().isEmpty()) {
			JsonArray reqs = new JsonArray(getRequirements().size());
			for (String req : getRequirements()) {
				reqs.add(req);
			}
			obj.add("requirements", reqs);
		}
		log(obj);
	}

	protected void logSuccess(String msg, Map<String, Object> map) {
		Map<String, Object> copy = new HashMap<>(map); // don't modify the underlying map
		copy.put("msg", msg);
		copy.put("result", "SUCCESS");
		if (!getRequirements().isEmpty()) {
			copy.put("requirements", getRequirements());
		}
		log(map);
	}
	
	/*
	 * Automatically log failures or warnings, depending on if this is an optional test
	 */
	
	protected void logFailure(JsonObject in) {
		JsonObject obj = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		obj.addProperty("result", optional ? "WARNING" : "FAILURE");
		log(obj);
	}
	
	protected void logFailure(String msg) {
		if (getRequirements().isEmpty()) {
			log(args("msg", msg, "result", optional ? "WARNING" : "FAILURE"));
		} else {
			log(args("msg", msg, "result", optional ? "WARNING" : "FAILURE", "requirements", getRequirements()));
		}
	}
	
	protected void logFailure(Map<String, Object> map) {
		Map<String, Object> copy = new HashMap<>(map); // don't modify the underlying map
		copy.put("result", optional ? "WARNING" : "FAILURE");
		if (!getRequirements().isEmpty()) {
			copy.put("requirements", getRequirements());
		}
		log(copy);
	}
	
	protected void logFailure(String msg, JsonObject in) {
		JsonObject copy = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		copy.addProperty("msg", msg);
		copy.addProperty("result", optional ? "WARNING" : "FAILURE");
		if (!getRequirements().isEmpty()) {
			JsonArray reqs = new JsonArray(getRequirements().size());
			for (String req : getRequirements()) {
				reqs.add(req);
			}
			copy.add("requirements", reqs);
		}
		log(copy);
	}

	protected void logFailure(String msg, Map<String, Object> map) {
		Map<String, Object> copy = new HashMap<>(map); // don't modify the underlying map
		copy.put("msg", msg);
		copy.put("result", optional ? "WARNING" : "FAILURE");
		if (!getRequirements().isEmpty()) {
			copy.put("requirements", getRequirements());
		}
		log(copy);
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
		logFailure(cause != null ? cause.getMessage() : "Error");
		throw new ConditionError(testId, getMessage(), cause);
	}
	
	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(String message, Throwable cause, Map<String, Object> map) {
		logFailure(message, map);
		throw new ConditionError(testId, getMessage() + ": " + message, cause);
	}

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(String message, Map<String, Object> map) {
		logFailure(message, map);
		throw new ConditionError(testId, getMessage() + ": " + message);
	}

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(Throwable cause, Map<String, Object> map) {
		logFailure(map);
		throw new ConditionError(testId, getMessage(), cause);
	}
	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(String message, Throwable cause, JsonObject in) {
		logFailure(message, in);
		throw new ConditionError(testId, getMessage() + ": " + message, cause);
	}

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(String message, JsonObject in) {
		logFailure(message, in);
		throw new ConditionError(testId, getMessage() + ": " + message);
	}

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(Throwable cause, JsonObject in) {
		logFailure(cause != null ? cause.getMessage() : "Error", in);
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
			log(msg, args("upload", RandomStringUtils.randomAlphanumeric(10), "result", "REVIEW"));
		} else {
			log(msg, args("upload", RandomStringUtils.randomAlphanumeric(10), "result", "REVIEW", "requirements", getRequirements()));
		}
	}
	
	protected void createUploadPlaceholder() {
		if (getRequirements().isEmpty()) {
			log(args("upload", RandomStringUtils.randomAlphanumeric(10), "result", "REVIEW"));
		} else {
			log(args("upload", RandomStringUtils.randomAlphanumeric(10), "result", "REVIEW", "requirements", getRequirements()));
		}
	}
	
	protected Map<String, Object> args(Object... a) {
		if (a == null || (a.length % 2) != 0) {
			throw new IllegalArgumentException("Need an even and nonzero number of arguments");
		}
		
		HashMap<String, Object> m = new HashMap<>(a.length / 2);
		
		for (int i = 0; i < a.length; i += 2) {
			String key = (String) a[i];
			Object val = a[i + 1];
			m.put(key, val);
		}
		
		return m;
	}
}
