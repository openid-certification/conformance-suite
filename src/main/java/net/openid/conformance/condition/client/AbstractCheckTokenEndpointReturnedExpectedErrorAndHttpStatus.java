package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Objects;

public abstract class AbstractCheckTokenEndpointReturnedExpectedErrorAndHttpStatus extends AbstractCondition {

	protected abstract Map<String, Integer> getErrorStatusMap();

	protected void verifyThatErrorIsExpectedAndMatchesStatusCode(String error, Integer httpStatus) throws ConditionError {
		Map<String, Integer> errorStatusMap = getErrorStatusMap();
		if (!errorStatusMap.containsKey(error)) {
			throw error("Unexpected error '" + error + "' received", args("actual", error, "expected", errorStatusMap.keySet()));
		}
		Integer expectedHttpStatus = getErrorStatusMap().get(error);
		if (!Objects.equals(httpStatus, expectedHttpStatus)) {
			throw error("Invalid http status with error " + error , args("actual", httpStatus, "expected", expectedHttpStatus));
		}
	}

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		Integer httpStatus = env.getInteger("token_endpoint_response_http_status");

		if (httpStatus == null) {
			throw error("Http status can not be null.");
		}

		String error = env.getString("token_endpoint_response", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("Couldn't find error field");
		}

		verifyThatErrorIsExpectedAndMatchesStatusCode(error, httpStatus);

		logSuccess("Token endpoint returned error " + error + " and the http status code was " + httpStatus);

		return env;
	}
}
