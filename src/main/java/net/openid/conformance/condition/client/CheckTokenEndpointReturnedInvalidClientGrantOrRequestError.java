package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.http.HttpStatus;

public class CheckTokenEndpointReturnedInvalidClientGrantOrRequestError extends AbstractCondition {

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
		} else if (error.equals("invalid_grant")) {

			httpStatusExpected = HttpStatus.SC_BAD_REQUEST;
			if (httpStatus != HttpStatus.SC_BAD_REQUEST) {

				throw error("Invalid http status with error invalid_grant", args("actual", httpStatus, "expected", httpStatusExpected));
			}
		} else if (error.equals("invalid_client")) {

			httpStatusExpected = HttpStatus.SC_UNAUTHORIZED;
			if (httpStatus != HttpStatus.SC_UNAUTHORIZED) {

				throw error("Invalid http status with error invalid_client", args("actual", httpStatus, "expected", httpStatusExpected));
			}
		} else {

			throw error("invalid_request, invalid_grant or invalid_client error was expected", args("actual", error));
		}

		logSuccess("Token endpoint returned error "+error+" and the http status code was " + httpStatusExpected);

		return env;
	}
}
