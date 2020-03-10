package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractAddClaimToAuthorizationEndpointRequest extends AbstractCondition {
	public enum LocationToRequestClaim {
		ID_TOKEN,
		USERINFO
	}

	public Environment addClaim(Environment env, LocationToRequestClaim locationToRequestClaim, String claim, String value, boolean essential) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		String locationStr;
		switch (locationToRequestClaim) {
			case ID_TOKEN:
				locationStr = "id_token";
				break;
			case USERINFO:
				locationStr = "userinfo";
				break;
			default:
				throw error("Unknown locationToRequestClaim value, this is a bug in the test condition");
		}

		JsonObject claims;
		if (authorizationEndpointRequest.has("claims")) {
			JsonElement claimsElement = authorizationEndpointRequest.get("claims");
			if (claimsElement.isJsonObject()) {
				claims = claimsElement.getAsJsonObject();
			} else {
				throw error("Invalid claims entry in authorization_endpoint_request", args("authorization_endpoint_request", authorizationEndpointRequest));
			}
		} else {
			claims = new JsonObject();
			authorizationEndpointRequest.add("claims", claims);
		}

		JsonObject claimsIdToken;
		if (claims.has(locationStr)) {
			JsonElement idTokenElement = claims.get(locationStr);
			if (idTokenElement.isJsonObject()) {
				claimsIdToken = idTokenElement.getAsJsonObject();
			} else {
				throw error("Invalid "+locationStr+" entry in authorization_endpoint_request", args("authorization_endpoint_request", authorizationEndpointRequest));
			}
		} else {
			claimsIdToken = new JsonObject();
			claims.add(locationStr, claimsIdToken);
		}

		JsonObject claimBody = new JsonObject();
		if (value != null) {
			claimBody.addProperty("value", value);
		}
		claimBody.addProperty("essential", essential);
		claimsIdToken.add(claim, claimBody);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added "+claim+" claim to authorization_endpoint_request", args("authorization_endpoint_request", authorizationEndpointRequest));

		return env;
	}
}
