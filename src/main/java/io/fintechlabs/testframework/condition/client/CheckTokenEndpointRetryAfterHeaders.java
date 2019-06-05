package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import org.apache.http.HttpHeaders;

public class CheckTokenEndpointRetryAfterHeaders extends AbstractCondition {

	@Override
	@PreEnvironment( required = "token_endpoint_response_headers" )
	public Environment evaluate(Environment env) {
		JsonObject headers = env.getObject("token_endpoint_response_headers");

		if (headers == null) {
			throw error("Headers token endpoint can not be null");
		}

		if (!headers.has(HttpHeaders.RETRY_AFTER)) {
			throw error("Couldn't find 'Retry-After' in the headers of token_endpoint_response");
		}

		logSuccess("Checked 'Retry-After' in the headers of token_endpoint_response.");

		return env;
	}
}
