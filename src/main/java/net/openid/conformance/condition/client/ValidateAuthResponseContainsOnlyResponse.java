package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateAuthResponseContainsOnlyResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "original_authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject response = env.getObject("original_authorization_endpoint_response");

		if (!response.has("response")) {
			throw error("'response' parameter missing from incoming response", args("response", response));
		}

		if (response.size() != 1) {
			throw error("Incoming response contains parameters other than 'response'", args("response", response));
		}

		logSuccess("Incoming response contains only expected 'response' parameter", response);

		return env;
	}

}
