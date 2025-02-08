package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateDirectPostResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "direct_post_response")
	public Environment evaluate(Environment env) {
		JsonElement responseEl = env.getElementFromObject("direct_post_response", "body_json");
		if (responseEl == null || !responseEl.isJsonObject()) {
			throw error("Direct post response is not a JSON object");
		}
		JsonObject response = responseEl.getAsJsonObject();

		if (response.isEmpty()) {
			logSuccess("Direct post response is an empty object.");
			return env;
		}

		if (response.size() == 1 && response.has("redirect_uri")) {
			logSuccess("Direct post response contains only 'redirect_uri'.");
			return env;
		}

		throw error("Direct post response object contains unexpected keys",
			args("response", response));
	}
}
