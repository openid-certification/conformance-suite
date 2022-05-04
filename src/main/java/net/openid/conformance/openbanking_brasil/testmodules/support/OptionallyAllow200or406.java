package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.http.HttpStatus;

public class OptionallyAllow200or406 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {

		int statusCode = env.getInteger("resource_endpoint_response_full", "status");

		if(statusCode == HttpStatus.SC_OK) {
			logSuccess(" endpoint returned an http status of 200 - proceeding with test now", args("http_status", statusCode));
			env.putString("proceed_with_test", "proceed");
		}

		if(statusCode == HttpStatus.SC_NOT_ACCEPTABLE) {
			logSuccess(" endpoint returned an http status of 422 - validating response and ending test now", args("http_status", statusCode));
		}

		if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_NOT_ACCEPTABLE) {
			throw error(" endpoint returned an unexpected http status - either 200 or 406 accepted", args("http_status", statusCode));
		}

		logSuccess(" endpoint returned the expected http status", args("http_status", statusCode));

		return env;
	}
}
