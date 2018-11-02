package io.fintechlabs.testframework.testmodule;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;

/**
 * Static implementation of data manipulation utility methods.
 *
 * @author jricher
 *
 */
public interface DataUtils {

	/**
	 * Utility function to convert an incoming multi-value map to a JSonObject for storage.
	 * this will throw out any duplicated headers.
	 */
	public default JsonObject mapToJsonObject(MultiValueMap<String, String> params, boolean lowercase) {
		JsonObject o = new JsonObject();
		for (String key : params.keySet()) {
			o.addProperty(
				lowercase ? key.toLowerCase() : key,
				params.getFirst(key));
		}
		return o;
	}

	public default Map<String, Object> args(Object... a) {
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

	public default Map<String, String> stringMap(Object... a) {
		if (a == null || (a.length % 2) != 0) {
			throw new IllegalArgumentException("Need an even and nonzero number of arguments");
		}

		// start with an empty map of the right size
		HashMap<String, String> m = new HashMap<>(a.length / 2);

		for (int i = 0; i < a.length; i += 2) {
			String key = (String) a[i];
			String val = (String)a[i + 1];
			m.put(key, val);
		}

		return m;
	}

	public default Map<String, Object> ex(Throwable cause) {
		return ex(cause, new HashMap<>());
	}

	public default JsonObject ex(Throwable cause, JsonObject in) {
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
		copy.addProperty("result", ConditionResult.FAILURE.toString());
		if (!copy.has("msg")) {
			copy.addProperty("msg", "unexpected exception caught");
		}

		return copy;
	}

	public default Map<String, Object> ex(Throwable cause, Map<String, Object> in) {
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
		if (!in.containsKey("msg")) {
			event.put("msg", "unexpected exception caught");
		}

		return event;
	}

	public default HttpHeaders headersFromJson(JsonObject headerJson) {
		HttpHeaders headers = new HttpHeaders();
		if (headerJson != null) {
			for (String header : headerJson.keySet()) {
				headers.set(header, headerJson.get(header).getAsString());
			}
		}
		return headers;
	}

}
