package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class ValidateKSAConsentRequestSignature extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "client_public_jwks", "parsed_client_request_jwt" })
	public Environment evaluate(Environment env) {
		String jwtString = env.getString("parsed_client_request_jwt", "value");
		JsonObject clientJwks = env.getObject("client_public_jwks");
		verifyJwsSignature(jwtString, clientJwks, "jwt", false, "client");
		return env;
	}
}
