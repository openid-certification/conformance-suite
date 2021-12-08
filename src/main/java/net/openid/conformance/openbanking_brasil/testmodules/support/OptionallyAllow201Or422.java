package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.http.HttpStatus;

public class OptionallyAllow201Or422 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		int statusCode = env.getInteger("endpoint_response", "status");
		String endpointName = env.getString("endpoint_response", "endpoint_name");

		if(statusCode == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
			logSuccess(endpointName + " endpoint returned an http status of 422 - ending test now", args("http_status", statusCode));
		}

		if(statusCode == HttpStatus.SC_CREATED) {
			logSuccess(endpointName + " endpoint returned an http status of 201 - proceeding with test now", args("http_status", statusCode));
			env.putString("proceed_with_test", "proceed");
		}

		if (statusCode != HttpStatus.SC_CREATED && statusCode != HttpStatus.SC_UNPROCESSABLE_ENTITY) {
			throw error(endpointName + " endpoint returned an unexpected http status - either 201 or 422 accepted", args("http_status", statusCode));
		}

		logSuccess(endpointName + " endpoint returned the expected http status", args("http_status", statusCode));

		return env;

	}

}
