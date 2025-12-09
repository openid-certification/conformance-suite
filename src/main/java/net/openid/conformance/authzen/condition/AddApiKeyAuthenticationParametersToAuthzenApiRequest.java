package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddApiKeyAuthenticationParametersToAuthzenApiRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "pdp")
	@PostEnvironment(required = "authzen_api_endpoint_request_headers")
	public Environment evaluate(Environment env) {
		String secret = env.getString("pdp", "api_key");

		if (secret == null) {
			throw error("API key not found in configuration");
		}

		JsonObject headers = env.getObject("authzen_api_endpoint_request_headers");

		if (headers == null) {
			headers = new JsonObject();
			env.putObject("authzen_api_endpoint_request_headers", headers);
		}

		headers.addProperty("Authorization", "Bearer " + secret);

		logSuccess("Added Bearer authorization header", headers);

		return env;
	}

}
