package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckIfTokenEndpointResponseError extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("token_endpoint_response")) {
			throw error("Couldn't find token endpoint response");
		}

		if (!Strings.isNullOrEmpty(env.getString("token_endpoint_response", "error"))) {
			throw error("Token endpoint error response", env.getObject("token_endpoint_response"));
		} else {
			logSuccess("No error from token endpoint");
			return env;
		}

	}

}
