package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForAccessTokenValue extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		if (Strings.isNullOrEmpty(env.getString("token_endpoint_response", "access_token"))) {
			throw error("access_token is missing or empty in token endpoint response");
		}
		if (Strings.isNullOrEmpty(env.getString("token_endpoint_response", "token_type"))) {
			throw error("token_type is missing or empty in token endpoint response");
		}

		logSuccess("Found an access token",
				args("access_token", env.getString("token_endpoint_response", "access_token")));
		return env;
	}

}
