package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidatePaymentConsentSignature extends AbstractVerifyJwsSignatureUsingKid {

	@Override
	@PreEnvironment(required = { "consent_endpoint_response_jwt", "org_server_jwks" })
	public Environment evaluate(Environment env) {

		String idToken = env.getString("consent_endpoint_response_jwt", "value");
		JsonObject serverJwks = env.getObject("org_server_jwks"); // to validate the signature

		verifyJwsSignature(idToken, serverJwks, "consent_endpoint_response_jwt", true, "organization");

		return env;
	}

}
