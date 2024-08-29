package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SignRequestObjectIncludeX5cHeaderIfAvailable extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "request_object_claims", "client_jwks" })
	@PostEnvironment(strings = "request_object")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("request_object_claims");
		JsonObject jwks = env.getObject("client_jwks");
		boolean includeX5c = true;
		boolean errorIfX5cMissing = false;
		return signJWT(env, claims, jwks, false, false, includeX5c, errorIfX5cMissing);
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("request_object", jws);
		logSuccess("Signed the request object", args("request_object", verifiableObj,
			"header", header,
			"claims", claimSet,
			"key", jwk));
	}

}
