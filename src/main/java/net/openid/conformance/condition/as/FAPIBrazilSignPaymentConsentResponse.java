package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilSignPaymentConsentResponse extends AbstractSignJWT {


	@Override
	@PreEnvironment(required = { "consent_response", "server_jwks"})
	@PostEnvironment(strings = "signed_consent_response")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("consent_response");
		JsonObject jwks = env.getObject("server_jwks");
		env = signJWT(env, claims, jwks, true);
		return env;
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("signed_consent_response", jws);
		logSuccess("Signed the payment consent response", args("signed_consent_response", verifiableObj));
	}

}
