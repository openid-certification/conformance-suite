package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * Check if there was an error from the authorization endpoint. If so, log the error and quit. If not, pass.
 */
public class CheckIfAuthorizationEndpointError extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		if (!Strings.isNullOrEmpty(env.getString("authorization_endpoint_response", "error"))) {
			throw error("Error from the authorization endpoint", env.getObject("authorization_endpoint_response"));
		}

		logSuccess("No error from authorization endpoint");
		return env;

	}

}
