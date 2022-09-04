package net.openid.conformance.condition.client;

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

		JsonObject requestObjectClaims = authorizationEndpointRequest.deepCopy();

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Created request object claims", args("request_object_claims", requestObjectClaims));

		return env;
	}

}
