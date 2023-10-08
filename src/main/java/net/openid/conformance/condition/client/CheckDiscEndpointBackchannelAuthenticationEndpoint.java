package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckDiscEndpointBackchannelAuthenticationEndpoint extends AbstractJsonUriIsValidAndHttps {

	private static final String environmentVariable = "backchannel_authentication_endpoint";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable);

	}
}
