package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class EnsureResponseCodeWas404 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {

		int statusCode = env.getInteger("resource_endpoint_response_full", "status");
		String endpointName = env.getString("resource_endpoint_response_full", "endpoint_name");

		if(statusCode != HttpStatus.NOT_FOUND.value()) {
			throw error(endpointName + " endpoint returned an unexpected http status - should be 404", args("http_status", statusCode));
		}

		logSuccess(endpointName + " endpoint returned the expected http status", args("http_status", statusCode));

		return env;

	}
}
