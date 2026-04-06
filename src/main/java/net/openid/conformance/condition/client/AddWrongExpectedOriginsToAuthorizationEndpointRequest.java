package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Adds an incorrect expected_origins value to the authorization request.
 * For signed DC API requests, the wallet must validate expected_origins
 * and reject requests with values that don't match the actual origin.
 */
public class AddWrongExpectedOriginsToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	public Environment evaluate(Environment env) {

		JsonObject request = env.getObject("authorization_endpoint_request");

		JsonArray wrongOrigins = new JsonArray();
		wrongOrigins.add("https://wrong.example.com");

		request.add("expected_origins", wrongOrigins);

		log("Added wrong expected_origins to authorization request",
			args("expected_origins", wrongOrigins));

		return env;
	}
}
