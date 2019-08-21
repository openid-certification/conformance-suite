package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

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
