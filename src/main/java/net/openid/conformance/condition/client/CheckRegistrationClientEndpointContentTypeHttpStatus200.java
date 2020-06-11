package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class CheckRegistrationClientEndpointContentTypeHttpStatus200 extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		Integer httpStatus = env.getInteger("registration_client_endpoint_response", "status");

		HttpStatus expectedStatus = HttpStatus.OK;

		if (httpStatus == null) {
			throw error("Http status can not be null.");
		}

		int expectedValue = expectedStatus.value();

		if (httpStatus != expectedValue) {
			throw error("Invalid http status", args("actual", httpStatus, "expected", expectedValue));
		}

		logSuccess("registration_client_endpoint_response http status code was " + expectedValue);

		return env;
	}
}
