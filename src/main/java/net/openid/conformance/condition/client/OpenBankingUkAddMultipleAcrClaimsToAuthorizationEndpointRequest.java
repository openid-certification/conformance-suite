package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OpenBankingUkAddMultipleAcrClaimsToAuthorizationEndpointRequest extends AbstractCondition {

	public Environment addsClaim(Environment env, String claim, JsonArray values, boolean essential) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		JsonObject claims;
		if (authorizationEndpointRequest.has("claims")) {
			JsonElement claimsElement = authorizationEndpointRequest.get("claims");
			if (!claimsElement.isJsonObject()) {
				throw error("Invalid claims in request", args("authorization_endpoint_request", authorizationEndpointRequest));
			}
			claims = claimsElement.getAsJsonObject();
		} else {
			claims = new JsonObject();
			authorizationEndpointRequest.add("claims", claims);
		}

		JsonObject claimsIdToken;
		if (claims.has("id_token")) {
			JsonElement idTokenElement = claims.get("id_token");
			if (!idTokenElement.isJsonObject()) {
				throw error("Invalid id_token in request claims", args("authorization_endpoint_request", authorizationEndpointRequest));
			}
			claimsIdToken = idTokenElement.getAsJsonObject();
		} else {
			claimsIdToken = new JsonObject();
			claims.add("id_token", claimsIdToken);
		}

		JsonObject claimBody = new JsonObject();
		claimBody.add("values", values);
		claimBody.addProperty("essential", essential);
		claimsIdToken.add(claim, claimBody);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added "+claim+" to request as an essential id_token claim", args("authorization_endpoint_request", authorizationEndpointRequest));

		return env;
	}

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonArray claims = new JsonArray();
		claims.add("urn:openbanking:psd2:sca");
		claims.add("urn:openbanking:psd2:ca");

		return addsClaim(env, "acr", claims, true);
	}

}
