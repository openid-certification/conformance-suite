package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddCodeChallengeToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"code_challenge","code_challenge_method"}, required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		String code_challenge = env.getString("code_challenge");
		if (Strings.isNullOrEmpty(code_challenge)) {
			throw error("Couldn't find code_challenge value");
		}

		String code_challenge_method = env.getString("code_challenge_method");
		if (Strings.isNullOrEmpty(code_challenge)) {
			throw error("Couldn't find code_challenge_method value");
		}

		if (!env.containsObject("authorization_endpoint_request")) {
			throw error("Couldn't find authorization endpoint request");
		}

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("code_challenge", code_challenge);
		authorizationEndpointRequest.addProperty("code_challenge_method", code_challenge_method);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added code_challenge and code_challenge_method parameters to request", authorizationEndpointRequest);

		return env;

	}

}
