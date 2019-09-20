package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class SignRequestObject extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "request_object_claims", "client_jwks" })
	@PostEnvironment(strings = "request_object")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("request_object_claims");
		JsonObject jwks = env.getObject("client_jwks");
		return signJWT(env, claims, jwks);
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("request_object", jws);
		logSuccess("Signed the request object", args("request_object", verifiableObj,
			"header", header.toString(),
			"claims", claimSet.toString(),
			"key", jwk.toJSONString()));
	}

}
