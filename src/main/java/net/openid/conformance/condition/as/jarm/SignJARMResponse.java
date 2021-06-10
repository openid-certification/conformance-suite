package net.openid.conformance.condition.as.jarm;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;

public class SignJARMResponse extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "jarm_response_claims", "server_jwks" })
	@PostEnvironment(strings = "jarm_response")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("jarm_response_claims");
		JsonObject jwks = env.getObject("server_jwks");
		return signJWT(env, claims, jwks);
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("jarm_response", jws);
		logSuccess("Signed the JARM response", args("jarm_response", verifiableObj));
	}

}
