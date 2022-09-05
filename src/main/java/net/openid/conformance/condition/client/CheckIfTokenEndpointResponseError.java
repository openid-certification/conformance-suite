package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckIfTokenEndpointResponseError extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("token_endpoint_response")) {
			throw error("Couldn't find token endpoint response");
		}

		if (!Strings.isNullOrEmpty(env.getString("token_endpoint_response", "error"))) {
			throw error("The token endpoint call was expected to succeed, but it returned an error response", env.getObject("token_endpoint_response"));
		} else {
			logSuccess("No error from token endpoint");
			return env;
		}

	}

}
