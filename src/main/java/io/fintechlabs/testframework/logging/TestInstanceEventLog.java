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

package io.fintechlabs.testframework.logging;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A wrapper around an EventLog that remebers the test ID and Owner information so the caller doesn't need to
 * 
 * @author jricher
 *
 */
public class TestInstanceEventLog {

	private String testId;
	private Map<String, String> owner;
	private EventLog eventLog;

	/**
	 * @param testId
	 * @param owner
	 * @param eventLog
	 */
	public TestInstanceEventLog(String testId, Map<String, String> owner, EventLog eventLog) {
		this.testId = testId;
		this.owner = owner;
		this.eventLog = eventLog;
	}

	/**
	 * @param source
	 * @param msg
	 * @see io.fintechlabs.testframework.logging.EventLog#log(java.lang.String, java.lang.String, java.util.Map, java.lang.String)
	 */
	public void log(String source, String msg) {
		eventLog.log(testId, source, owner, msg);
	}

	/**
	 * @param source
	 * @param obj
	 * @see io.fintechlabs.testframework.logging.EventLog#log(java.lang.String, java.lang.String, java.util.Map, com.google.gson.JsonObject)
	 */
	public void log(String source, JsonObject obj) {
		eventLog.log(testId, source, owner, obj);
	}

	/**
	 * @param source
	 * @param map
	 * @see io.fintechlabs.testframework.logging.EventLog#log(java.lang.String, java.lang.String, java.util.Map, java.util.Map)
	 */
	public void log(String source, Map<String, Object> map) {
		eventLog.log(testId, source, owner, map);
	}
	
	public static Map<String, Object> args(Object... a) {
		if (a == null || (a.length % 2) != 0) {
			throw new IllegalArgumentException("Need an even and nonzero number of arguments");
		}
		
		// start with an empty map of the right size
		HashMap<String, Object> m = new HashMap<>(a.length / 2);
		
		for (int i = 0; i < a.length; i += 2) {
			String key = (String) a[i];
			Object val = a[i + 1];
			m.put(key, val);
		}
		
		return m;
	}
	
		
	public static Map<String, Object> ex(Throwable cause) {
		return ex(cause, new HashMap<>());
	}
	
	public static Map<String, Object> ex(Throwable cause, Map<String, Object> in) {
		if (cause == null) {
			return null;
		}
		
		Map<String, Object> event = new HashMap<>(in);
		event.put("error", cause.getMessage());
		event.put("error_class", cause.getClass().getName());
		
		List<String> stack = Arrays.stream(cause.getStackTrace())
			.map(StackTraceElement::toString)
			.collect(Collectors.toList());

		event.put("stacktrace", stack);

		return event;
	}
	
	public static JsonObject ex(Throwable cause, JsonObject in) {
		JsonObject copy = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		copy.addProperty("error", cause.getMessage());
		copy.addProperty("error_class", cause.getClass().getName());
		
		JsonArray stack = Arrays.stream(cause.getStackTrace())
			.map(StackTraceElement::toString)
			.collect(() -> new JsonArray(cause.getStackTrace().length),
					(c, e) -> c.add(e),
					(c1, c2) -> c1.addAll(c2));
		
		copy.add("stacktrace", stack);

		return copy;
	}
	
}
