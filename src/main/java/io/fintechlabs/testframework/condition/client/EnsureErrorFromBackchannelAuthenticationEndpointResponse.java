package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureErrorFromBackchannelAuthenticationEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {

		String error = env.getString("backchannel_authentication_endpoint_response", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("Expected 'error' field not found");
		}

		logSuccess("Back channel authentication endpoint returned an 'error'", args("error", error));

		return env;
	}
}
