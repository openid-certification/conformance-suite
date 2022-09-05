package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractValidateResponseCacheHeaders;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckTokenEndpointCacheHeaders extends AbstractValidateResponseCacheHeaders {

	@Override
	@PreEnvironment( required = "token_endpoint_response_headers" )
	public Environment evaluate(Environment env) {
		JsonObject headers = env.getObject("token_endpoint_response_headers");

		String humanReadableResponseName = "token endpoint response";

		validateCacheHeaders(headers, humanReadableResponseName);

		return env;
	}

}
