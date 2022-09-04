package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class EnsureParHTTPError extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		Integer status = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status");
		if (status == null) {
			throw error("PAR http status not found in environment");
		}

		if (status < 400 || status >= 600) {
			throw error("Invalid pushed authorization request endpoint response http status code",
				args("expected", "4xx or 5xx", "actual", status));
		}

		logSuccess("Pushed Authorization Request Endpoint returned a HTTP 4xx or 5xx error as expected", args("actual", status));
		return env;

	}
}
