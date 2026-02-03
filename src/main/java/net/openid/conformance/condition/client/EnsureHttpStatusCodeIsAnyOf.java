package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

public class EnsureHttpStatusCodeIsAnyOf extends AbstractCondition {

	private final Set<Integer> statusCodes;

	public EnsureHttpStatusCodeIsAnyOf(Integer ... statusCodes) {
		this.statusCodes = Set.of(statusCodes);
	}

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		int statusCode = env.getInteger("endpoint_response", "status");
		String endpointName = env.getString("endpoint_response", "endpoint_name");

		if (!statusCodeMatches(statusCode)) {
			throw error(endpointName + " endpoint returned an unexpected http status",
				args("http_status", statusCode, "expected_status_codes", getExpectedStatusCodes()));
		}

		logSuccess(createSuccessMessage(endpointName, statusCode),
			args(
				"http_status", statusCode,
				"expected_status_codes", getExpectedStatusCodes()));

		return env;

	}

	protected String createSuccessMessage(String endpointName, int statusCode) {
		return endpointName + " endpoint returned an expected http status.";
	}

	protected boolean statusCodeMatches(int statusCode) {
		return statusCodes.contains(statusCode);
	}

	protected Set<Integer> getExpectedStatusCodes() {
		return statusCodes;
	}
}
