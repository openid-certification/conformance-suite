package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CheckDiscRequirePushedAuthorizationRequestsNotSet extends AbstractCondition {

	private static final String discoveryKey = "require_pushed_authorization_requests";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement el = env.getElementFromObject("server", discoveryKey);

		if (el == null) {
			logSuccess(discoveryKey + " is not present in server configuration.");
			return env;
		}

		if (!el.getAsJsonPrimitive().isBoolean()) {
			throw error("Type of " + discoveryKey + " must be boolean.");
		}

		if (OIDFJSON.getBoolean(el)) {
			throw error(discoveryKey + " is set.");
		}

		logSuccess(discoveryKey + " is not set.");

		return env;
	}

}
