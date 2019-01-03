package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckTokenEndpointReturnedJsonContentType extends AbstractCondition {

	public CheckTokenEndpointReturnedJsonContentType(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "token_endpoint_response_headers")
	public Environment evaluate(Environment env) {
		JsonObject responseHeaders = env.getObject("token_endpoint_response_headers");

		String contentType = responseHeaders.getAsJsonPrimitive("content-type").getAsString();
		String mimeType = contentType.split(";")[0].trim();
		String expected = "application/json";
		if (mimeType.equals(expected)) {
			logSuccess("Token endpoint Content-Type: header is " + expected);
			return env;
		}

		throw error("Invalid content-type header in token endpoint response", args("expected",	 expected, "actual", contentType));
	}

}
