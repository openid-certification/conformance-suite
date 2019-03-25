package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckBackchannelAuthenticationEndpointContentType extends AbstractCondition {

	public CheckBackchannelAuthenticationEndpointContentType(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response_headers")
	public Environment evaluate(Environment env) {

		String contentType = env.getString("backchannel_authentication_endpoint_response_headers", "content-type");
		String mimeType = contentType.split(";")[0].trim();
		String expected = "application/json";

		if (Strings.isNullOrEmpty(mimeType) || !mimeType.equals(expected)) {
			throw error("Invalid content-type header in backchannel authentication endpoint response", args("expected",	 expected, "actual", contentType));

		}

		logSuccess("Backchannel authentication endpoint Content-Type: header is " + expected);
		return env;
	}
}
