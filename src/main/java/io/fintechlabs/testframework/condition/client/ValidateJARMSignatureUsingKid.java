package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateJARMSignatureUsingKid extends ValidateIdTokenSignatureUsingKid {

	@Override
	@PreEnvironment(required = { "jarm_response", "server_jwks" })
	public Environment evaluate(Environment env) {

		String jarmResponse = env.getString("jarm_response", "value");
		JsonObject serverJwks = env.getObject("server_jwks");

		validateTokenSignature(jarmResponse, serverJwks, "jarm_response");

		return env;
	}


}
