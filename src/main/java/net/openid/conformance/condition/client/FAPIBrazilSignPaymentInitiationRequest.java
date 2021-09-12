package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilSignPaymentInitiationRequest extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "resource_request_entity_claims", "client" })
	@PostEnvironment(strings = "resource_request_entity" )
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("resource_request_entity_claims");
		JsonObject jwks = (JsonObject) env.getElementFromObject("client", "org_jwks");
		return signJWT(env, claims, jwks, true); // typ explicitly required in Brazil spec
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("resource_request_entity", jws);
		logSuccess("Signed the request", args("request", verifiableObj,
			"header", header.toString(),
			"claims", claimSet.toString(),
			"key", jwk.toJSONString()));
	}

}
