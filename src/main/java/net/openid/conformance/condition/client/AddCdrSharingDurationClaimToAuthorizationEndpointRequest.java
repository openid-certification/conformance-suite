package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddCdrSharingDurationClaimToAuthorizationEndpointRequest extends AbstractAddClaimToAuthorizationEndpointRequest {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

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

		// no particular reason for this value; it's just the one in the example at https://consumerdatastandardsaustralia.github.io/standards/#request-object
		claims.addProperty("sharing_duration", 7776000);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added sharing_duration claim to authorization_endpoint_request", args("authorization_endpoint_request", authorizationEndpointRequest));

		return env;
	}

}
