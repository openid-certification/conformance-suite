package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * Ensure there was an error from the authorization endpoint. If there isn't, log it and quit.
 */
public class EnsureAuthorizationEndpointError extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment in) {
		if (!in.containsObject("authorization_endpoint_response")) {
			throw error("Couldn't find callback parameters");
		}

		if (!Strings.isNullOrEmpty(in.getString("authorization_endpoint_response", "error"))) {
			logSuccess("Error from the authorization endpoint", in.getObject("authorization_endpoint_response"));
			return in;
		} else {
			throw error("No error from authorization endpoint");
		}

	}

}
