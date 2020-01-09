package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateErrorDescriptionFromAuthorizationEndpointResponseError extends AbstractValidateErrorDescriptionFromResponseError {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		return validateErrorDescription(env, "authorization_endpoint_response");
	}

}
