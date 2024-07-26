package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateUserInfoResponseSignature extends AbstractVerifyJwsSignature {

	private static final String USERINFO_ENDPOINT_RESPONSE = "userinfo_endpoint_response_full";

	@Override
	@PreEnvironment(required = {"server_jwks", USERINFO_ENDPOINT_RESPONSE})
	public Environment evaluate(Environment env) {
		String userInfoStr = env.getString(USERINFO_ENDPOINT_RESPONSE, "body");

		JsonObject serverJwks = env.getObject("server_jwks"); // to validate the signature

		verifyJwsSignature(userInfoStr, serverJwks, USERINFO_ENDPOINT_RESPONSE, false, "server");

		return env;
	}

}
