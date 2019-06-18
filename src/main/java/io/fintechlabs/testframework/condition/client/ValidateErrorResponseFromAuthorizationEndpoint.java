package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class ValidateErrorResponseFromAuthorizationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject callbackParams = env.getObject("authorization_endpoint_response");
		int requiredParameterCount = 0;
		int optionalParameterCount = 0;

		// https://openid.net/specs/openid-connect-core-1_0.html#AuthError
		if (callbackParams.has("error")) {
			requiredParameterCount++;

			// state can be absented if authorization request did not send state in the request object
			// but, it is one of the required field in authorization response
			if (callbackParams.has("state")) {
				requiredParameterCount++;
			}

			// Now, count the optional parameters
			if (callbackParams.has("error_description")) {
				optionalParameterCount++;
			}
			if (callbackParams.has("error_uri")) {
				optionalParameterCount++;
			}

			// Check the number of keys we've found, and can accept, against the total.
			if (callbackParams.keySet().size() - requiredParameterCount - optionalParameterCount == 0) {
				logSuccess("error response includes only expected parameters", callbackParams);
			} else {
				throw error("error response includes unexpected parameters", callbackParams);
			}
		} else {
			throw error("Authorization server was expected to return an error but did not", callbackParams);
		}
		return env;
	}

}
