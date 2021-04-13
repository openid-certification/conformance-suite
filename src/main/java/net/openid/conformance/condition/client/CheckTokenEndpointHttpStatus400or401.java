package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class CheckTokenEndpointHttpStatus400or401 extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		Integer httpStatus = env.getInteger("token_endpoint_response_http_status");

		if (httpStatus == null) {
			throw error("Http status can not be null.");
		}

		if (httpStatus != 400 && httpStatus != 401) {
			throw error("Invalid http status", args("actual", httpStatus, "expected", "400 or 401"));
		}

		logSuccess("Token endpoint http status code was " + httpStatus);

		return env;
	}
}
