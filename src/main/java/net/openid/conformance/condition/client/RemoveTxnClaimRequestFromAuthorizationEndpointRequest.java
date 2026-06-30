package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveTxnClaimRequestFromAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		JsonElement idTokenClaimsElement = env.getElementFromObject(
			"authorization_endpoint_request", "claims.id_token");

		if (idTokenClaimsElement != null && idTokenClaimsElement.isJsonObject()) {
			idTokenClaimsElement.getAsJsonObject().remove("txn");
		}

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);
		logSuccess("Removed txn from the requested ID Token claims",
			args("authorization_endpoint_request", authorizationEndpointRequest));
		return env;
	}
}
