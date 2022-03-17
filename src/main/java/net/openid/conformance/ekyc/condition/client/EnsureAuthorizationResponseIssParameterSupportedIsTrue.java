package net.openid.conformance.ekyc.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractValidateJsonBoolean;
import net.openid.conformance.testmodule.Environment;

public class EnsureAuthorizationResponseIssParameterSupportedIsTrue extends AbstractValidateJsonBoolean {
	private static final String environmentVariable = "authorization_response_iss_parameter_supported";
	private static final boolean requiredValue = true;
	private static final boolean defaultValue = false;

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, defaultValue, requiredValue);

	}

}
