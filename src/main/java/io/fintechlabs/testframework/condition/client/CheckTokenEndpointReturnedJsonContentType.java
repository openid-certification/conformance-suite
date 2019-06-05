package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class CheckTokenEndpointReturnedJsonContentType extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response_headers")
	public Environment evaluate(Environment env) {
		JsonObject responseHeaders = env.getObject("token_endpoint_response_headers");

		String contentType = OIDFJSON.getString(responseHeaders.get("content-type"));
		String mimeType = contentType.split(";")[0].trim();
		String expected = "application/json";
		if (mimeType.equals(expected)) {
			logSuccess("Token endpoint Content-Type: header is " + expected);
			return env;
		}

		throw error("Invalid content-type header in token endpoint response", args("expected",	 expected, "actual", contentType));
	}

}
