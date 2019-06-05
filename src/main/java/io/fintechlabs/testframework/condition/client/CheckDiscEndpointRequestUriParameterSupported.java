package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckDiscEndpointRequestUriParameterSupported extends ValidateJsonBoolean {

	private static final String environmentVariable = "request_uri_parameter_supported";
	private static final boolean requiredValue = true;
	private static final boolean defaultValue = true;

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, defaultValue, requiredValue);

	}

}
