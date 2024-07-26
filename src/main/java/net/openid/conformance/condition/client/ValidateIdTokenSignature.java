package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateIdTokenSignature extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "id_token", "server_jwks" })
	public Environment evaluate(Environment env) {

		String idToken = env.getString("id_token", "value");
		JsonObject serverJwks = env.getObject("server_jwks"); // to validate the signature

		verifyJwsSignature(idToken, serverJwks, "id_token", false, "server");

		return env;
	}

}
