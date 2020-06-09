package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractExtractJWT;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractLogoutTokenFromBackchannelLogoutRequest extends AbstractExtractJWT {

	@Override
	@PreEnvironment(required = "backchannel_logout_request")
	@PostEnvironment(required = "logout_token")
	public Environment evaluate(Environment env) {

		return extractJWT(env, "backchannel_logout_request", "body_form_params.logout_token", "logout_token");

	}

}
