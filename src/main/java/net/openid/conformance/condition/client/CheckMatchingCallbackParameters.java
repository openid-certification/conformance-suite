package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

public class CheckMatchingCallbackParameters extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "redirect_uri", required = "callback_query_params")
	public Environment evaluate(Environment env) {

		UriComponents redirectUri = UriComponentsBuilder.fromUriString(env.getString("redirect_uri")).build();

		Map<String, String> params = redirectUri.getQueryParams().toSingleValueMap();

		JsonObject o = new JsonObject(); // For the log

		for (Map.Entry<String, String> entry : params.entrySet()) {
			String key = entry.getKey();
			String expected = entry.getValue();
			String actual = env.getString("callback_query_params", key);

			if (!expected.equals(actual)) {
				throw error("The client should have been registered with a redirect uri that contains ?dummy1=lorem&dummy2=ipsum (as per instructions), and this url was passed as the redirect uri to the authorization endpoint. These parameters must be present in the redirect back, but they are not.", args("parameter", key, "expected", expected, "actual", actual));
			}

			o.addProperty(key, expected);
		}

		logSuccess("Callback parameters successfully verified", o);

		return env;
	}

}
