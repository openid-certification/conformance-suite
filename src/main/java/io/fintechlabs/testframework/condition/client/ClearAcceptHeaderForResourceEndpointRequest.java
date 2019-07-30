package io.fintechlabs.testframework.condition.client;

import org.springframework.http.HttpHeaders;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class ClearAcceptHeaderForResourceEndpointRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		if (requestHeaders != null) {
			requestHeaders.remove(HttpHeaders.ACCEPT);
		}

		logSuccess("Clearing custom Accept header");

		return env;
	}

}
