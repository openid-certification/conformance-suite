package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;


public class EnsureResponseCodeWas201or200 extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		int statusCode = env.getInteger("resource_endpoint_response_status");

		if(statusCode == org.apache.http.HttpStatus.SC_OK) {
			logSuccess("endpoint returned an http status of 200 - ending test now", args("http_status", statusCode));
		} else if(statusCode == org.apache.http.HttpStatus.SC_CREATED) {
			logSuccess("endpoint returned an http status of 201 - proceeding with test now", args("http_status", statusCode));
			env.putString("proceed_with_test", "proceed");
		} else {
			throw error("endpoint returned an unexpected http status - either 201 or 200 accepted", args("http_status", statusCode));
		}

		logSuccess("endpoint returned the expected http status", args("http_status", statusCode));

		return env;
	}
}
