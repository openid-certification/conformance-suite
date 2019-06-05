package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.Arrays;

public class FAPIRWCheckDiscEndpointTokenEndpointAuthMethodsSupported extends ValidateJsonArray {

	private static final String environmentVariable = "token_endpoint_auth_methods_supported";

	private static final String[] SET_VALUES = new String[] { "private_key_jwt", "tls_client_auth" };

	private static final String errorMessageNotEnough = "No matching value from server";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), 1,
			errorMessageNotEnough);
	}

}
