package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SignFakeIdToken extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "id_token_claims", "client_jwks", "id_token" })
	@PostEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("id_token_claims");
		JsonObject jwks = env.getObject("client_jwks");
		return signJWT(env, claims, jwks);
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		JsonObject idTokenObj = env.getObject("id_token");
		idTokenObj.addProperty("value", jws);
		env.putObject("id_token", idTokenObj);
		logSuccess("Signed a 'fake' ID token using the client's keys", args("id_token", verifiableObj));
	}

}
