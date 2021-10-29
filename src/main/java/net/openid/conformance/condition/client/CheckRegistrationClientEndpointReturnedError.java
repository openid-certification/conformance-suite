package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckRegistrationClientEndpointReturnedError extends AbstractCondition {

	@Override
	@PreEnvironment(required = "registration_client_endpoint_response")
	public Environment evaluate(Environment env) {

		String error = env.getString("registration_client_endpoint_response", "body_json.error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("'error' field not found in response from registration client endpoint response");
		}

		logSuccess("Registration client endpoint returned 'error' field", args("error", error));

		return env;
	}
}
