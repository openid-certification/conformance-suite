package net.openid.conformance.condition.rs;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureIncomingRequestMethodIsGet extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		String method = env.getString("incoming_request", "method");

		if (!"GET".equalsIgnoreCase(method)) {
			throw error("This endpoint requires http GET method", args("actual_http_method", method));
		}
		logSuccess("Client correctly used http GET method");
		return env;
	}

}
