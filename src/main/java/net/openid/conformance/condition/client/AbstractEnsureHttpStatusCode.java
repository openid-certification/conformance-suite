package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractEnsureHttpStatusCode extends AbstractCondition {
	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		int statusCode = env.getInteger("endpoint_response", "status");
		String endpointName = env.getString("endpoint_response", "endpoint_name");

		if (statusCode!= getExpectedStatusCode()) {
			throw error(endpointName + " endpoint returned an unexpected http status",
				args("http_status", statusCode, "expected_status", getExpectedStatusCode()));
		}

		logSuccess(endpointName + " endpoint returned the expected http status",
			args(
				"http_status", statusCode,
				"expected_status", getExpectedStatusCode()));

		return env;

	}

	protected abstract int getExpectedStatusCode();
}
