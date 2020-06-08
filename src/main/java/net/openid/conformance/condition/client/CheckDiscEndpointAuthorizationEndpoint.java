package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckDiscEndpointAuthorizationEndpoint extends AbstractJsonUriIsValidAndHttps {

	private static final String environmentVariable = "authorization_endpoint";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable);

	}
}
