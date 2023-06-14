package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateResourceResponseSignature extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "endpoint_response_jwt", "org_server_jwks" })
	public Environment evaluate(Environment env) {

		String idToken = env.getString("endpoint_response_jwt", "value");
		JsonObject serverJwks = env.getObject("org_server_jwks"); // to validate the signature

		verifyJwsSignature(idToken, serverJwks, "endpoint_response_jwt", true, "organization");

		return env;
	}

}
