package net.openid.conformance.testmodule;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import net.openid.conformance.condition.Condition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Static implementation of data manipulation utility methods.
 */
public interface DataUtils {

	// The FAPI implementers draft 2 specs explicitly require the charset UTF8 header, but Spring have correctly
	// deprecated this in their http module, so we have our own definition to ensure we follow the specs.
	// https://github.com/spring-projects/spring-framework/issues/22788
	// https://bitbucket.org/openid/fapi/issues/236/charset-not-needed-for-application-json
	MediaType DATAUTILS_MEDIATYPE_APPLICATION_JSON_UTF8 = new MediaType("application", "json",StandardCharsets.UTF_8);
	MediaType DATAUTILS_MEDIATYPE_APPLICATION_JWT_UTF8 = new MediaType("application", "jwt",StandardCharsets.UTF_8);
	MediaType DATAUTILS_MEDIATYPE_APPLICATION_JOSE = new MediaType("application", "jose");

	/**
	 * Utility function to convert an incoming multi-value map to a JSonObject for storage.
	 * this will throw out any duplicated headers.
	 */
	public default JsonObject mapToJsonObject(MultiValueMap<String, String> params, boolean lowercase) {
		JsonObject o = new JsonObject();
		for (String key : params.keySet()) {
			List<String> values = params.get(key);
			if (values != null && values.size() > 1) {
				o.add(
					lowercase ? key.toLowerCase() : key,
					new Gson().toJsonTree(values));
			} else {
				o.addProperty(
					lowercase ? key.toLowerCase() : key,
					params.getFirst(key));
			}
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

	public default JsonObject ex(Throwable exception, JsonObject in) {
		JsonObject copy = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		copy.addProperty("error", exception.getMessage());
		copy.addProperty("error_class", exception.getClass().getName());

		Throwable cause = exception.getCause();
		if (cause != null) {
			copy.addProperty("cause", cause.getMessage());
			copy.addProperty("cause_class", cause.getClass().getName());

			JsonArray causeStack = Arrays.stream(cause.getStackTrace())
				.map(StackTraceElement::toString)
				.collect(JsonArray::new,
					JsonArray::add,
					JsonArray::addAll);

			copy.add("cause_stacktrace", causeStack);

		}

		JsonArray stack = Arrays.stream(exception.getStackTrace())
			.map(StackTraceElement::toString)
			.collect(JsonArray::new,
				JsonArray::add,
				JsonArray::addAll);

		copy.add("stacktrace", stack);
		copy.addProperty("result", Condition.ConditionResult.FAILURE.toString());
		if (!copy.has("msg")) {
			copy.addProperty("msg", "unexpected exception caught: "+exception.getMessage());
		}

		return copy;
	}

	public default Map<String, Object> ex(Throwable exception, Map<String, Object> in) {
		if (exception == null) {
			return null;
		}

		Map<String, Object> event = new HashMap<>(in);
		event.put("error", exception.getMessage());
		event.put("error_class", exception.getClass().getName());

		Throwable cause = exception.getCause();
		if (cause != null) {
			event.put("cause", cause.getMessage());
			event.put("cause_class", cause.getClass().getName());
			List<String> causeStack = Arrays.stream(cause.getStackTrace())
				.map(StackTraceElement::toString)
				.collect(Collectors.toList());

			event.put("cause_stacktrace", causeStack);
		}

		List<String> stack = Arrays.stream(exception.getStackTrace())
			.map(StackTraceElement::toString)
			.collect(Collectors.toList());

		event.put("stacktrace", stack);
		event.put("result", Condition.ConditionResult.FAILURE);
		if (!in.containsKey("msg")) {
			event.put("msg", "unexpected exception caught: "+exception.getMessage());
		}

		return event;
	}

	public default HttpHeaders headersFromJson(JsonObject headerJson) {
		HttpHeaders headers = new HttpHeaders();
		if (headerJson != null) {
			for (String header : headerJson.keySet()) {
				headers.set(header, OIDFJSON.getString(headerJson.get(header)));
			}
		}
		return headers;
	}

}
