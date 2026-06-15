package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class SignKSAConsentRequest extends AbstractSignJWT {

	@Override
	protected String performSigning(JWSHeader header, JsonObject claims, JWSSigner signer) throws JOSEException, ParseException {
		return performSigningEnsureAudIsArray(header, claims, signer);
	}

	@Override
	@PreEnvironment(required = { "account_requests_endpoint_request", "client" })
	@PostEnvironment(strings = "account_requests_endpoint_request_signed")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("account_requests_endpoint_request");
		JsonObject jwks = (JsonObject) env.getElementFromObject("client", "jwks");
		return signJWT(env, claims, jwks);
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("account_requests_endpoint_request_signed", jws);
		logSuccess("Signed the KSA consent request", args("request", verifiableObj,
			"header", header.toString(),
			"claims", claimSet.toString()));
	}
}
