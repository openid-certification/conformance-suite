package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CheckDiscRequirePushedAuthorizationRequestsIsTrue extends AbstractCondition {

	private static final String discoveryKey = "require_pushed_authorization_requests";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement el = env.getElementFromObject("server", discoveryKey);

		if (el == null) {
			throw error(discoveryKey + " is not present in server configuration; it must be set to true.");
		}

		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
			throw error(discoveryKey + " is not a boolean value.", args(discoveryKey, el));
		}

		if (!OIDFJSON.getBoolean(el)) {
			throw error(discoveryKey + " must be set to true.", args(discoveryKey, el));
		}

		logSuccess(discoveryKey + " is true.");

		return env;
	}

}
