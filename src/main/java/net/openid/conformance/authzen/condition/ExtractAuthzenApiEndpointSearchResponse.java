package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractAuthzenApiEndpointSearchResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response")
	@PostEnvironment(required = "authzen_search_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement bodyElem = env.getElementFromObject("authzen_api_endpoint_response", "body_json");
		if (bodyElem == null) {
			throw error("No json response from AuthZEN API endpoint");
		}
		if (!bodyElem.isJsonObject()) {
			throw error("AuthZEN API search response is not a JSON object",
				args("body_json", bodyElem));
		}
		JsonObject search = bodyElem.getAsJsonObject();

		env.putObject("authzen_search_endpoint_response", search);

		logSuccess("Extracted search response from API endpoint response",
			args("Search response", search));

		return env;
	}

}
