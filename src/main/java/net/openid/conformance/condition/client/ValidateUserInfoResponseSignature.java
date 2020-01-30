package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateUserInfoResponseSignature extends AbstractVerifyJwsSignature {

	public static final String USERINFO_ENDPOINT_RESPONSE = "userinfo_endpoint_response";

	@Override
	@PreEnvironment(required = "server_jwks", strings= USERINFO_ENDPOINT_RESPONSE)
	public Environment evaluate(Environment env) {
		String userInfoStr = env.getString(USERINFO_ENDPOINT_RESPONSE);

		JsonObject serverJwks = env.getObject("server_jwks"); // to validate the signature

		verifyJwsSignature(userInfoStr, serverJwks, USERINFO_ENDPOINT_RESPONSE);

		return env;
	}

}
