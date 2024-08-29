package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;

public class SignIdTokenWithX5tS256 extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "id_token_claims", "server_jwks" })
	@PostEnvironment(strings = "id_token")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("id_token_claims");
		JsonObject jwks = env.getObject("server_jwks");
		boolean includeTyp = false;
		boolean includeX5tS256 = true;
		boolean includeX5c = false;
		boolean errorIfX5cMissing = false;
		return signJWT(env, claims, jwks, includeTyp, includeX5tS256, includeX5c, errorIfX5cMissing);
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("id_token", jws);
		logSuccess("Signed the ID token", args("id_token", verifiableObj));
	}

}
