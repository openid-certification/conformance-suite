package net.openid.conformance.condition.client;

import java.util.Map;

import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;

public class CheckMatchingCallbackParameters extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "redirect_uri", required = "callback_query_params")
	public Environment evaluate(Environment env) {

		UriComponents redirectUri = UriComponentsBuilder.fromHttpUrl(env.getString("redirect_uri")).build();

		Map<String, String> params = redirectUri.getQueryParams().toSingleValueMap();

		JsonObject o = new JsonObject(); // For the log

		for (Map.Entry<String, String> entry : params.entrySet()) {
			String key = entry.getKey();
			String expected = entry.getValue();
			String actual = env.getString("callback_query_params", key);

			if (!expected.equals(actual)) {
				throw error("Callback parameter invalid or missing", args("parameter", key, "expected", expected, "actual", actual));
			}

			o.addProperty(key, expected);
		}

		logSuccess("Callback parameters successfully verified", o);

		return env;
	}

}
