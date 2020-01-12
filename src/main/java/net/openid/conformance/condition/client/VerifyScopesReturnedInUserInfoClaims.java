package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VerifyScopesReturnedInUserInfoClaims extends AbstractVerifyScopesReturnedInClaims {

	@Override
	@PreEnvironment(required = { "userinfo", "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {
		JsonElement claims = env.getObject("userinfo");
		return verifyScopesInClaims(env, claims, "userinfo");
	}
}
