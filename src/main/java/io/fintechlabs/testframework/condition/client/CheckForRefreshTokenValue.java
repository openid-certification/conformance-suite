package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForRefreshTokenValue extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("token_endpoint_response", "refresh_token"))) {
			logSuccess("Found a refresh token",
				args("refresh_token", env.getString("token_endpoint_response", "refresh_token")));
			return env;
		} else {
			throw error("Couldn't find refresh token");
		}
	}

}
