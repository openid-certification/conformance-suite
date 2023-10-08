package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractExtractJWT;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractIdTokenHintFromBackchannelEndpointRequest extends AbstractExtractJWT {

	@Override
	@PreEnvironment(required = "backchannel_request_object")
	@PostEnvironment(required = "id_token_hint")
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
		JsonObject clientJwks = env.getObject("client_jwks");
		return extractJWT(env, "backchannel_request_object", "claims.id_token_hint", "id_token_hint", client, clientJwks);
	}

}
