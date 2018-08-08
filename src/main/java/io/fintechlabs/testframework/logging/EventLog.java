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
import io.fintechlabs.testframework.condition.Condition.ConditionResult;

/**
 * @author jricher
 *
 */
public interface EventLog {

	/**
	 * @param testId
	 *            The instance identifier of the test
	 * @param source
	 *            The source of the event
	 * @param owner
	 *            The owner of the test run
	 * @param msg
	 *            The message to log
	 */
	void log(String testId, String source, Map<String, String> owner, String msg);

	/**
	 * @param testId
	 *            The instance identifier of the test
	 * @param source
	 *            The source of the event
	 * @param owner
	 *            The owner of the test run
	 * @param obj
	 *            The message to log
	 */
	void log(String testId, String source, Map<String, String> owner, JsonObject obj);

	/**
	 * @param testId
	 *            The instance identifier of the test
	 * @param source
	 *            The source of the event
	 * @param owner
	 *            The owner of the test run
	 * @param map
	 *            The message to log
	 */
	void log(String testId, String source, Map<String, String> owner, Map<String, Object> map);

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

	public static JsonObject ex(Throwable cause, JsonObject in) {
		JsonObject copy = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		copy.addProperty("error", cause.getMessage());
		copy.addProperty("error_class", cause.getClass().getName());

		if (cause.getCause() != null) {
			copy.addProperty("cause", cause.getCause().getMessage());
			copy.addProperty("cause_class", cause.getCause().getClass().getName());
		}

		JsonArray stack = Arrays.stream(cause.getStackTrace())
			.map(StackTraceElement::toString)
			.collect(() -> new JsonArray(cause.getStackTrace().length),
				(c, e) -> c.add(e),
				(c1, c2) -> c1.addAll(c2));

		copy.add("stacktrace", stack);

		return copy;
	}

	public static Map<String, Object> ex(Throwable cause, Map<String, Object> in) {
		if (cause == null) {
			return null;
		}

		Map<String, Object> event = new HashMap<>(in);
		event.put("error", cause.getMessage());
		event.put("error_class", cause.getClass().getName());

		if (cause.getCause() != null) {
			event.put("cause", cause.getCause().getMessage());
			event.put("cause_class", cause.getCause().getClass().getName());
		}

		List<String> stack = Arrays.stream(cause.getStackTrace())
			.map(StackTraceElement::toString)
			.collect(Collectors.toList());

		event.put("stacktrace", stack);
		event.put("result", ConditionResult.FAILURE);
		event.put("msg", "unexpected exception caught");

		return event;
	}

	/**
	 * Start a new log block and return its ID
	 *
	 * @return
	 */
	String startBlock();

	/**
	 * end a log block and return the previous block ID
	 */
	String endBlock();

}
