package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckDiscRequirePushedAuthorizationRequestsIsABoolean extends AbstractCondition {

	private static final String discoveryKey = "require_pushed_authorization_requests";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement el = env.getElementFromObject("server", discoveryKey);

		if (el == null) {
			logSuccess(discoveryKey + " is not present in server configuration.");
			return env;
		}

		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
			throw error(discoveryKey + " is not a boolean value.", args(discoveryKey, el));
		}

		logSuccess(discoveryKey + " is a boolean value.");

		return env;
	}

}
