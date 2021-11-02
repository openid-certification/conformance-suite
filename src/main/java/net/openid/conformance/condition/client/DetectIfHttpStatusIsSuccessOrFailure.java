package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class DetectIfHttpStatusIsSuccessOrFailure extends AbstractCondition {
	public static String endpointResponseWas2xx = "endpoint_response_was_2xx";

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {
		env.putBoolean(endpointResponseWas2xx, false);

		int statusCode = env.getInteger("endpoint_response", "status");
		String endpointName = env.getString("endpoint_response", "endpoint_name");

		if (statusCode >= 200 && statusCode <= 299) {
			env.putBoolean(endpointResponseWas2xx, true);
			logSuccess(endpointName + " is permitted to return success or failure for this test. As the returned http status ("+statusCode+") is a 2xx status, assuming the call succeeded.");
		} else {
			logSuccess(endpointName + " is permitted to return success or failure for this test. As the returned http status (" + statusCode + ") is NOT a 2xx status, assuming the call failed.");
		}

		return env;

	}
}
