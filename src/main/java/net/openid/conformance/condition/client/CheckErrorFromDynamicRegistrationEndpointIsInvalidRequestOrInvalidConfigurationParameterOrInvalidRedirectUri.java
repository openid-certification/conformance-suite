package net.openid.conformance.condition.client;

import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckErrorFromDynamicRegistrationEndpointIsInvalidRequestOrInvalidConfigurationParameterOrInvalidRedirectUri extends AbstractCondition {

	private static final List<String> PERMITTED_ERRORS = ImmutableList.of("invalid_request", "invalid_configuration_parameter", "invalid_redirect_uri");

	@Override
	@PreEnvironment(required = "dynamic_registration_endpoint_response")
	public Environment evaluate(Environment env) {

		String error = env.getString("dynamic_registration_endpoint_response", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("'error' field not found in response from dynamic registration endpoint");
		}

		if (!PERMITTED_ERRORS.contains(error)) {
			throw error("'error' field has unexpected value", args("permitted", PERMITTED_ERRORS, "actual", error));
		}

		logSuccess("Dynamic registration endpoint returned 'error'", args("permitted", PERMITTED_ERRORS, "error", error));

		return env;
	}
}
