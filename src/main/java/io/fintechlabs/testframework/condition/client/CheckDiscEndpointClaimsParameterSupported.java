package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckDiscEndpointClaimsParameterSupported extends ValidateJsonBoolean {

	private static final String environmentVariable = "claims_parameter_supported";
	private static final boolean requiredValue = true;
	private static final boolean defaultValue = false;

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, defaultValue, requiredValue);

	}

}
