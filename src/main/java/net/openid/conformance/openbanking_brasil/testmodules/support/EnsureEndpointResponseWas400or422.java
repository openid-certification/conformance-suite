package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.http.HttpStatus;

public class EnsureEndpointResponseWas400or422 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		int statusCode = env.getInteger("endpoint_response", "status");
		String endpointName = env.getString("endpoint_response", "endpoint_name");

		if(statusCode != HttpStatus.SC_BAD_REQUEST && statusCode != HttpStatus.SC_UNPROCESSABLE_ENTITY) {
			throw error(endpointName + " endpoint returned an unexpected http status - should be 400 or 422", args("http_status", statusCode));
		}

		if(statusCode == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
			env.putString("proceed_with_test", "true");
		}

		logSuccess(endpointName + " endpoint returned the expected http status", args("http_status", statusCode));

		return env;

	}

}
