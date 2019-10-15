package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SignClientAuthenticationAssertion extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "client_assertion_claims", "client_jwks" })
	@PostEnvironment(strings = "client_assertion")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("client_assertion_claims");
		JsonObject jwks = env.getObject("client_jwks");
		return signJWT(env, claims, jwks);
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("client_assertion", jws);
		logSuccess("Signed the client assertion", args("client_assertion", verifiableObj));
	}

}
