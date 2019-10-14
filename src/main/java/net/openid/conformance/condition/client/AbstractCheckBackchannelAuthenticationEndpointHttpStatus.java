package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public abstract class AbstractCheckBackchannelAuthenticationEndpointHttpStatus extends AbstractCondition {

	protected abstract HttpStatus getExpectedHttpStatus();

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
