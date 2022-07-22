package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.http.HttpStatus;

public class EnsureEndpointResponseWas400or422or201 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		int statusCode = env.getInteger("endpoint_response", "status");
		String endpointName = env.getString("endpoint_response", "endpoint_name");

		if(statusCode != HttpStatus.SC_BAD_REQUEST && statusCode != HttpStatus.SC_UNPROCESSABLE_ENTITY && statusCode != HttpStatus.SC_CREATED) {
			throw error(endpointName + " endpoint returned an unexpected http status - should be 400, 422 or 201", args("http_status", statusCode));
		}

		if(statusCode == HttpStatus.SC_BAD_REQUEST) {
			if(env.getString("proceed_with_test") != null) {
				env.removeNativeValue("proceed_with_test");
			}
		}
		logSuccess(endpointName + " endpoint returned the expected http status", args("http_status", statusCode));

		return env;

	}

}
