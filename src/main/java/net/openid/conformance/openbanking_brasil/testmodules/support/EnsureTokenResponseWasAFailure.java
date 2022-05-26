package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureTokenResponseWasAFailure extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject tokenResponse = env.getObject("token_endpoint_response");

		String tokenString = tokenResponse.toString();
		if (tokenString.contains("access_token")) {
			throw error("Was expecting a failure but a new access_token was issued");
		}

		logSuccess("Call was a failure since no access token issued, as expected");
		return env;
	}

}
