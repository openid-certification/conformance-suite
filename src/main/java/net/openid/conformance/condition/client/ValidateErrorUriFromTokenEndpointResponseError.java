package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateErrorUriFromTokenEndpointResponseError extends AbstractValidateErrorUriFromResponseError {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		return checkErrorUri(env, "token_endpoint_response");
	}

}
