package net.openid.conformance.authzen.condition;

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

		JsonObject search = (JsonObject) env.getElementFromObject("authzen_api_endpoint_response", "body_json");
		if (search == null) {
			throw error("No json response from Authzen API endpoint");
		}

		env.putObject("authzen_search_endpoint_response", search);

		logSuccess("Extracted search response from API endpoint response",
			args("Search response", search));

		return env;
	}

}
