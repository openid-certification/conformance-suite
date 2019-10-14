package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateErrorResponseFromBackchannelAuthenticationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject callbackParams = env.getObject("backchannel_authentication_endpoint_response");

		int requiredParameterCount = 0;
		int optionalParameterCount = 0;

		if (callbackParams.has("error")) {

			requiredParameterCount++;

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
			throw error("No error parameter found", callbackParams);
		}

		return env;
	}
}
