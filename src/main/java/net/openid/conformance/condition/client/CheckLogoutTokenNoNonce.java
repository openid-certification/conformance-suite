package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckLogoutTokenNoNonce extends AbstractCondition {

	@Override
	@PreEnvironment(required = "logout_token" )
	public Environment evaluate(Environment env) {

		JsonElement nonce = env.getElementFromObject("logout_token", "claims.nonce");
		if (nonce != null) {
			throw error("Logout token has a nonce, which it must not.");
		}

		logSuccess("No nonce in logout token.");

		return env;

	}

}
