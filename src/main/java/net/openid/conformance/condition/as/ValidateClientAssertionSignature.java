package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientAssertionSignature extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "client", "client_assertion" })
	public Environment evaluate(Environment env) {

		//TODO add the following
		/*
		token_endpoint_auth_signing_alg
		...
		All Token Requests using these authentication methods from this Client MUST be rejected,
		if the JWT is not signed with this algorithm
		...
		 */
		String clientAssertionString = env.getString("client_assertion", "value");
		JsonObject client = env.getObject("client");
		JsonObject clientJWKS = client.get("jwks").getAsJsonObject();
		verifyJwsSignature(clientAssertionString, clientJWKS, "client_assertion");

		return env;
	}
}
