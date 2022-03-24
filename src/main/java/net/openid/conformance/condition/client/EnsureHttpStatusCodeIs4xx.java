package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class EnsureHttpStatusCodeIs4xx extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		Integer httpStatus = env.getInteger("endpoint_response", "status");
		String endpointName = env.getString("endpoint_response", "endpoint_name");

		if (httpStatus == null) {
			throw error("Http status can not be null.");
		}

		if (httpStatus >= 400 && httpStatus <= 499) {
			logSuccess(endpointName + " endpoint http status code was " + httpStatus);
			return env;
		}

		throw error(endpointName + "endpoint returned a different http status than expected", args("actual", httpStatus, "expected", "400 to 499"));
	}
}
