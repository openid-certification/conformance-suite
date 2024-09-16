package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.hc.core5.http.HttpStatus;

import java.util.List;

public class CheckBackchannelAuthenticationEndpointErrorHttpStatus extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {
		Integer httpStatus = env.getInteger("backchannel_authentication_endpoint_response_http_status");

		if (httpStatus == null) {
			throw error("Http status can not be null.");
		}

		String error = env.getString("backchannel_authentication_endpoint_response", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("Couldn't find error field");
		}

		int httpStatusExpected;

		if (error.equals("invalid_request")) {

			httpStatusExpected = HttpStatus.SC_BAD_REQUEST;
			if (httpStatus != HttpStatus.SC_BAD_REQUEST) {

				throw error("Invalid http status with error invalid_request" , args("actual", httpStatus, "expected", httpStatusExpected));
			}
		} else if (error.equals("access_denied")) {

			httpStatusExpected = HttpStatus.SC_FORBIDDEN;
			if (httpStatus != HttpStatus.SC_FORBIDDEN) {

				throw error("Invalid http status with error access_denied", args("actual", httpStatus, "expected", httpStatusExpected));
			}
		} else if (error.equals("invalid_client")) {

			List<Integer> httpStatusesExpected = List.of(HttpStatus.SC_UNAUTHORIZED, HttpStatus.SC_BAD_REQUEST);
			if (!httpStatusesExpected.contains(httpStatus)) {

				throw error("Invalid http status with error invalid_client", args("actual", httpStatus, "expected", httpStatusesExpected));
			}
		} else {

			throw error("http status was not matching 400 or 403 or 401", args("actual", httpStatus));
		}

		logSuccess("Backchannel authentication endpoint http status code was " + httpStatus);

		return env;
	}
}
