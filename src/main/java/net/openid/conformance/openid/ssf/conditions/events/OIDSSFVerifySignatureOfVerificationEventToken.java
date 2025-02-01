package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFVerifySignatureOfVerificationEventToken extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = {"ssf", "server_jwks"})
	public Environment evaluate(Environment env) {

		String tokenString = env.getString("ssf", "verification.jwt");

		JsonObject serverJwks = env.getObject("server_jwks"); // to validate the signature
		verifyJwsSignature(tokenString, serverJwks, "SET", false, "server");
		logSuccess("Signature of verification SET is valid", args("token", tokenString));

		return env;
	}

}
