package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class FAPICIBAAddAcrValuesToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String requestedACRs = env.getString("client", "acr_value");

		if (requestedACRs == null) {
			throw error("Couldn't find acr_value in configuration");
		}

		// Check provided acr_values is supported by server or not
		boolean acrValueIsSupportedFlg = false;
		JsonArray acrValuesSupported = env.getElementFromObject("server", "acr_values_supported").getAsJsonArray();
		for (JsonElement jsonElementAcrValue : acrValuesSupported) {
			if (OIDFJSON.getString(jsonElementAcrValue).equals(requestedACRs)) {
				acrValueIsSupportedFlg = true;
				break;
			}
		}

		if (!acrValueIsSupportedFlg) {
			throw error("Provided acr value is not supported by server", args("supported_acr_values", acrValuesSupported, "received_value", requestedACRs));
		}

		authorizationEndpointRequest.addProperty("acr_values", requestedACRs);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess(String.format("Added acr_values of '%s' to authorization endpoint request", requestedACRs), authorizationEndpointRequest);

		return env;
	}
}
