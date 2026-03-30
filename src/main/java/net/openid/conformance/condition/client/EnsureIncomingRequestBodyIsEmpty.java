package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureIncomingRequestBodyIsEmpty extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		String body = env.getString("incoming_request", "body");

		if (body == null || body.isEmpty()) {
			logSuccess("Request body was correctly empty");
			return env;
		} else {
			throw error("Request body was not empty", args("body", body));
		}

	}

}
