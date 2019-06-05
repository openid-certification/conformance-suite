package io.fintechlabs.testframework.condition.client;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ConvertAuthorizationEndpointRequestToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		if (authorizationEndpointRequest == null) {
			throw error("Couldn't find authorization endpoint request");
		}

		JsonObject requestObjectClaims = new JsonObject();
		for (Map.Entry<String, JsonElement> entry : authorizationEndpointRequest.entrySet()) {
			requestObjectClaims.add(entry.getKey(), entry.getValue());
		}

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Created request object claims", args("request_object_claims", requestObjectClaims));

		return env;
	}

}
