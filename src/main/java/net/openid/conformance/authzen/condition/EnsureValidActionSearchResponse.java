package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureValidActionSearchResponse extends EnsureValidSearchResponse {

	@Override
	@PreEnvironment(required = {"authzen_search_endpoint_response"})
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected void ensureValidResponseResultsObject(JsonObject resultsObj) {
		if (!resultsObj.has("name")) {
			throw error("An action object in the results array does not contain a name", args("element", resultsObj));
		}
		JsonElement nameElem = resultsObj.get("name");
		if (!nameElem.isJsonPrimitive() || !nameElem.getAsJsonPrimitive().isString()) {
			throw error("An action object's name is not a string", args("element", resultsObj));
		}
		String name = OIDFJSON.getString(nameElem);
		if (name.isEmpty()) {
			throw error("An action object's name is an empty string", args("element", resultsObj));
		}
	}
}
