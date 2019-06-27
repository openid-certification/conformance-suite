package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import org.apache.http.HttpStatus;

public class CheckTokenEndpointHttpStatus401Or400 extends AbstractCondition {

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

		int httpStatusExpected;

		if (error.equals("invalid_request")) {

			httpStatusExpected = HttpStatus.SC_BAD_REQUEST;
			if (httpStatus != HttpStatus.SC_BAD_REQUEST) {

				throw error("Invalid http status with error invalid_request" , args("actual", httpStatus, "expected", httpStatusExpected));
			}
		} else if (error.equals("invalid_client")) {

			httpStatusExpected = HttpStatus.SC_UNAUTHORIZED;
			if (httpStatus != HttpStatus.SC_UNAUTHORIZED) {

				throw error("Invalid http status with error invalid_client", args("actual", httpStatus, "expected", httpStatusExpected));
			}
		} else {

			throw error("http status was not matching 400 or 401", args("actual", httpStatus));
		}

		logSuccess("Token endpoint http status code was " + httpStatusExpected);

		return env;
	}
}
