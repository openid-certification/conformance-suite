package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateErrorUriFromBackchannelAuthenticationEndpoint extends AbstractValidateErrorUriFromResponseError {

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {
		return checkErrorUri(env, "backchannel_authentication_endpoint_response");
	}

}
