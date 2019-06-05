package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForScopesInTokenResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("token_endpoint_response", "scope"))) {
			logSuccess("Found scopes returned with access token",
				args("scope", env.getString("token_endpoint_response", "scope")));
			return env;
		} else {
			throw error("Couldn't find scope");
		}
	}

}
