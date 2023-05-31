package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectNoIdTokenInTokenResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("token_endpoint_response");

		if (response != null && response.has("id_token")) {
			throw error("Test is not targeting Open ID Connect but the token endpoint response contains an ID token.");
		}

		logSuccess("Test is not targeting Open ID Connect and the token endpoint response does not contain an ID token");

		return env;
	}

}
