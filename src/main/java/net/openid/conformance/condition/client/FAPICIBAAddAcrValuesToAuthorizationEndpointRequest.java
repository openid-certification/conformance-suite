package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

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

		logSuccess("Added acr_values of '%s' to authorization endpoint request".formatted(requestedACRs), authorizationEndpointRequest);

		return env;
	}
}
