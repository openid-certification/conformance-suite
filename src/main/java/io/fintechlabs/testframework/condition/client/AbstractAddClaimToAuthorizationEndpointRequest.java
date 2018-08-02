package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public abstract class AbstractAddClaimToAuthorizationEndpointRequest extends AbstractCondition {
	public AbstractAddClaimToAuthorizationEndpointRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	public Environment addClaim(Environment env, String claim, String value, boolean essential) {
		JsonObject authorizationEndpointRequest = env.get("authorization_endpoint_request");

		JsonObject claims;
		if (authorizationEndpointRequest.has("claims")) {
			JsonElement claimsElement = authorizationEndpointRequest.get("claims");
			if (claimsElement.isJsonObject()) {
				claims = claimsElement.getAsJsonObject();
			} else {
				throw error("Invalid claims in request", args("authorization_endpoint_request", authorizationEndpointRequest));
			}
		} else {
			claims = new JsonObject();
			authorizationEndpointRequest.add("claims", claims);
		}

		JsonObject claimsIdToken;
		if (claims.has("id_token")) {
			JsonElement idTokenElement = claims.get("id_token");
			if (idTokenElement.isJsonObject()) {
				claimsIdToken = idTokenElement.getAsJsonObject();
			} else {
				throw error("Invalid id_token in request claims", args("authorization_endpoint_request", authorizationEndpointRequest));
			}
		} else {
			claimsIdToken = new JsonObject();
			claims.add("id_token", claimsIdToken);
		}

		JsonObject claimBody = new JsonObject();
		claimBody.addProperty("value",value);
		claimBody.addProperty("essential", essential);
		claimsIdToken.add(claim, claimBody);

		env.put("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added "+claim+" claim to request", args("authorization_endpoint_request", authorizationEndpointRequest));

		return env;
	}
}
