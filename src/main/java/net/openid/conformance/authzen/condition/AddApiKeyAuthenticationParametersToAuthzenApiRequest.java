package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddApiKeyAuthenticationParametersToAuthzenApiRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "authzen_api_endpoint_request_headers")
	public Environment evaluate(Environment env) {
		// A JSON null is a value not entered, as for a missing field.
		JsonElement value = env.getElementFromObject("client", "api_key");
		if (value == null || value.isJsonNull()) {
			throw error("'PDP API Key' field is missing from the 'Client' section in the test configuration");
		}
		if (!OIDFJSON.isString(value)) {
			throw error("'PDP API Key' field in the 'Client' section of the test configuration is not a string",
				args("api_key", value));
		}
		String secret = OIDFJSON.getString(value);
		if (secret.isBlank()) {
			// An empty key would send "Authorization: Bearer " and the PDP would reject a request
			// the test meant to authenticate.
			throw error("'PDP API Key' field in the 'Client' section of the test configuration is empty");
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
