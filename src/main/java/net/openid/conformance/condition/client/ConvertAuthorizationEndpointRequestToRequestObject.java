package net.openid.conformance.condition.client;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
