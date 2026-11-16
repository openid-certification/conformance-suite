package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureValidActionSearchResponse extends EnsureValidSearchResponse {

	@Override
	@PreEnvironment(required = {"authzen_search_endpoint_response"})
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected void ensureValidResponseResultsObject(JsonObject resultsObj) {
		getRequiredStringMember(resultsObj, "name", "An action object");
		ensurePropertiesIsObject(resultsObj, "An action object");
	}
}
