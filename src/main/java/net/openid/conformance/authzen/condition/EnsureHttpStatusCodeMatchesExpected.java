package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureHttpStatusCodeMatchesExpected extends AbstractCondition {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {
		Integer expectedObj = env.getInteger("authzen_expected_http_status_code");
		if (expectedObj == null) {
			throw error("Expected HTTP status code was not set in the environment");
		}
		int expected = expectedObj;
		int actual = env.getInteger("endpoint_response", "status");
		String endpointName = env.getString("endpoint_response", "endpoint_name");

		if (actual != expected) {
			throw error(endpointName + " endpoint returned an unexpected http status",
				args("http_status", actual, "expected_status", expected));
		}

		logSuccess(endpointName + " endpoint returned the expected http status",
			args("http_status", actual, "expected_status", expected));
		return env;
	}
}
