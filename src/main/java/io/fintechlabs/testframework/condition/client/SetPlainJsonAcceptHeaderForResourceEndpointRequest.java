package io.fintechlabs.testframework.condition.client;

import org.springframework.http.HttpHeaders;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class SetPlainJsonAcceptHeaderForResourceEndpointRequest extends AbstractCondition {

	public SetPlainJsonAcceptHeaderForResourceEndpointRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		if (requestHeaders == null) {
			requestHeaders = new JsonObject();
			env.putObject("resource_endpoint_request_headers", requestHeaders);
		}

		requestHeaders.addProperty(HttpHeaders.ACCEPT, "application/json");

		logSuccess("Set Accept header", args("Accept", requestHeaders.get("Accept")));

		return env;
	}

}
