package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureHttpStatusCodeMatchesExpected extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"endpoint_response", "authzen_expected_http_status_codes"})
	public Environment evaluate(Environment env) {
		List<Integer> acceptable = readAcceptableStatusCodes(env);
		int actual = env.getInteger("endpoint_response", "status");
		String endpointName = env.getString("endpoint_response", "endpoint_name");

		if (!acceptable.contains(actual)) {
			throw error(endpointName + " endpoint returned an unexpected http status",
				args("http_status", actual, "acceptable_statuses", acceptable));
		}

		logSuccess(endpointName + " endpoint returned an acceptable http status",
			args("http_status", actual, "acceptable_statuses", acceptable));
		return env;
	}

	private List<Integer> readAcceptableStatusCodes(Environment env) {
		JsonObject wrapper = env.getObject("authzen_expected_http_status_codes");
		if (wrapper == null || !wrapper.has("codes") || !wrapper.get("codes").isJsonArray()) {
			throw error("Expected HTTP status codes were not set in the environment");
		}
		JsonArray codes = wrapper.getAsJsonArray("codes");
		List<Integer> out = new ArrayList<>();
		for (JsonElement e : codes) {
			out.add(OIDFJSON.getInt(e));
		}
		if (out.isEmpty()) {
			throw error("Acceptable HTTP status code set is empty");
		}
		return out;
	}
}
