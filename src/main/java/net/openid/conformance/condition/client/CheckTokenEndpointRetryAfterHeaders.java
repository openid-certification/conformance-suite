package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.hc.core5.http.HttpHeaders;

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
