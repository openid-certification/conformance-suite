package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractExtractJWT;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractIdTokenFromTokenResponse extends AbstractExtractJWT {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	@PostEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {
		JsonObject clientJwks = env.getObject("client_jwks");
		// this passes 'client' as null, and hence doesn't currently support symmetric keys, as they're not allowed in
		// FAPI - we should add explicit checks so the FAPI tests fail if symmetric encryption is used, and then
		// enable symmetric encryption for the OIDCC tests
		return extractJWT(env, "token_endpoint_response", "id_token", "id_token", null, clientJwks);
	}

}
