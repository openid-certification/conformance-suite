package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckLogoutTokenHasSubOrSid extends AbstractCondition {

	@Override
	@PreEnvironment(required = "logout_token" )
	public Environment evaluate(Environment env) {

		JsonElement sub = env.getElementFromObject("logout_token", "claims.sub");
		JsonElement sid = env.getElementFromObject("logout_token", "claims.sid");
		if (sub == null && sid == null) {
			throw error("logout token has neither sub nor sid - it must have at least one of them.");
		}

		logSuccess("logout token contains sub and/or sid");

		return env;

	}

}
