package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;

public class SignIdToken extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "id_token_claims", "server_jwks" })
	@PostEnvironment(strings = "id_token")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("id_token_claims");
		JsonObject jwks = env.getObject("server_jwks");
		return signJWT(env, claims, jwks);
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("id_token", jws);
		logSuccess("Signed the ID token", args("id_token", verifiableObj));
	}

}
