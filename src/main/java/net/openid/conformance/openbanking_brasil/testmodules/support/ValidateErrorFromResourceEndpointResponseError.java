package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ValidateErrorFromResourceEndpointResponseError extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement errors = env.getElementFromObject("resource_endpoint_response", "errors");
		if (errors == null) {
			throw error("The server was expected to return an error, but the 'errors' field in the response is null");
		}

		JsonArray errorsArray = errors.getAsJsonArray();
		if(errorsArray.isEmpty()) {
			throw error("Errors JsonArray is empty.");
		}

		JsonObject errorsField = errorsArray.get(0).getAsJsonObject();

		Set<String> keySet = errorsField.keySet();
		HashSet<String> referenceSet = new HashSet<String>(Arrays.asList("code", "title", "detail"));
		if(!keySet.equals(referenceSet)){
			referenceSet.removeAll(keySet);
			throw error("One or more required fields are missing", args("missing fields", referenceSet));
		}

		logSuccess("ResponseError was successfully validated", args("ResponseError",errors));

		return env;
	}
}
