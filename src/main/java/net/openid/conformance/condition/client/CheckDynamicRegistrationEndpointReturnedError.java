package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckDynamicRegistrationEndpointReturnedError extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_endpoint_response")
	public Environment evaluate(Environment env) {

		String error = env.getString("dynamic_registration_endpoint_response", "body_json.error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("'error' field not found in response from dynamic registration endpoint");
		}

		logSuccess("Dynamic registration endpoint returned 'error' field", args("error", error));

		return env;
	}
}
