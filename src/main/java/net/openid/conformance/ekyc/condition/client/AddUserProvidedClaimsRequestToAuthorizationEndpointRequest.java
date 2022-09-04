package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddUserProvidedClaimsRequestToAuthorizationEndpointRequest extends AbstractCondition {


	@Override
	@PreEnvironment(required = {"server", "config", "authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement userProvidedClaimsRequest = env.getElementFromObject("config", "ekyc_verified_claims_request");
		if(userProvidedClaimsRequest==null){
			throw error("User provided verified claims request is not set in configuration");
		}
		JsonObject claims = userProvidedClaimsRequest.getAsJsonObject();
		JsonObject authzEndpointRequest = env.getObject("authorization_endpoint_request");
		authzEndpointRequest.add("claims", claims);
		logSuccess("Added user provided claims request to authorization request",
			args("authorization_endpoint_request",
				env.getObject("authorization_endpoint_request")));
		return env;
	}

}
