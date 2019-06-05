package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;
import org.springframework.http.HttpStatus;

public abstract class AbstractCheckTokenEndpointHttpStatus extends AbstractCondition {

	protected abstract HttpStatus getExpectedHttpStatus();

	@Override
	public Environment evaluate(Environment env) {
		Integer httpStatus = env.getInteger("token_endpoint_response_http_status");

		HttpStatus expectedStatus = getExpectedHttpStatus();

		if (httpStatus == null) {
			throw error("Http status can not be null.");
		}

		int expectedValue = expectedStatus.value();

		if (httpStatus != expectedValue) {
			throw error("Invalid http status", args("actual", httpStatus, "expected", expectedValue));
		}

		logSuccess("Token endpoint http status code was " + expectedValue);

		return env;
	}
}
