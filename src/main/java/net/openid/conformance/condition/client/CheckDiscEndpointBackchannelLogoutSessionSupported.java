package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckDiscEndpointBackchannelLogoutSessionSupported extends AbstractValidateJsonBoolean {

	private static final String environmentVariable = "backchannel_logout_session_supported";
	private static final boolean requiredValue = true;
	private static final boolean defaultValue = false;

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, defaultValue, requiredValue);

	}

}
