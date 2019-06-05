package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddAcrValuesScaToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String acrExpected = "urn:openbanking:psd2:sca";

		authorizationEndpointRequest.addProperty("acr_values", acrExpected);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess(String.format("Added acr_values of '%s' to authorization endpoint request", acrExpected), authorizationEndpointRequest);

		return env;
	}
}
