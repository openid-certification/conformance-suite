package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureHttpStatusCodeIs4xxOrFailedTLS extends AbstractCondition {

	public static final String RESPONSE_SSL_ERROR_KEY = "dynamic_registration_endpoint_response_ssl_error";

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {
		Integer httpStatus = env.getInteger("endpoint_response", "status");
		String endpointName = env.getString("endpoint_response", "endpoint_name");
		Boolean ssl_error = env.getBoolean(RESPONSE_SSL_ERROR_KEY);

		if (ssl_error == null) {
			throw error("Missing environment variable: " + RESPONSE_SSL_ERROR_KEY);
		}

		if (httpStatus == null) {
			if (ssl_error) {
				logSuccess("TSL has failed.");
				return env;
			}
			throw error("Http status can not be null.");
		}

		if (httpStatus >= 400 && httpStatus <= 499) {
			logSuccess(endpointName + " endpoint http status code was " + httpStatus);
			return env;
		}

		throw error(endpointName + "endpoint returned a different http status than expected", args("actual", httpStatus, "expected", "400 to 499"));
	}
}
