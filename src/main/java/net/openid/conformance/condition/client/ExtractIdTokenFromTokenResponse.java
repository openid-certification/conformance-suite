package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractExtractJWT;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractIdTokenFromTokenResponse extends AbstractExtractJWT {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	@PostEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {
		return extractJWT(env, "token_endpoint_response", "id_token", "id_token");
	}

}
