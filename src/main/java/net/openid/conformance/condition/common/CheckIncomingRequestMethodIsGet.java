package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckIncomingRequestMethodIsGet extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		String method = env.getString("incoming_request", "method");

		if (!method.equals("GET")) {
			throw error("The HTTP method used is not 'GET'", args("method", method));
		}

		logSuccess("HTTP method used is 'GET'");
		return env;
	}

}
