package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateJARMSignatureUsingKid extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "jarm_response", "server_jwks" })
	public Environment evaluate(Environment env) {

		String jarmResponse = env.getString("jarm_response", "value");
		JsonObject serverJwks = env.getObject("server_jwks");

		verifyJwsSignature(jarmResponse, serverJwks, "jarm_response", true, "server");

		return env;
	}


}
