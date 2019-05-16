package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.springframework.http.HttpStatus;

public abstract class AbstractCheckBackchannelAuthenticationEndpointHttpStatus extends AbstractCondition {

	protected abstract HttpStatus getExpectedHttpStatus();

	public AbstractCheckBackchannelAuthenticationEndpointHttpStatus(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	public Environment evaluate(Environment env) {
		Integer httpStatus = env.getInteger("backchannel_authentication_endpoint_response_http_status");

		HttpStatus expectedStatus = getExpectedHttpStatus();

		if (httpStatus == null) {
			throw error("Http status can not be null.");
		}

		int expectedValue = expectedStatus.value();

		if (httpStatus != expectedValue) {
			throw error("Invalid http status", args("actual", httpStatus, "expected", expectedValue));
		}

		logSuccess("Backchannel authentication endpoint http status code was " + expectedValue);

		return env;
	}
}
