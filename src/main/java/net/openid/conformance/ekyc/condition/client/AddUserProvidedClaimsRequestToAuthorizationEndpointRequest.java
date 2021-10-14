package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddUserProvidedClaimsRequestToAuthorizationEndpointRequest extends AbstractCondition {


	@Override
	@PreEnvironment(required = {"server", "config", "authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		//TODO Making this a string as a workaround until we can figure out why object properties with null values get lost
		JsonElement userProvidedClaimsRequest = env.getElementFromObject("config", "ekyc_verified_claims_request");
		String claimsStr = OIDFJSON.getString(userProvidedClaimsRequest);
		JsonObject claims = new JsonParser().parse(claimsStr).getAsJsonObject();
		JsonObject authzEndpointRequest = env.getObject("authorization_endpoint_request");
		authzEndpointRequest.add("claims", claims);
		logSuccess("Added user provided claims request to authorization request",
			args("authorization_endpoint_request",
				env.getObject("authorization_endpoint_request").toString()));
		return env;
	}

}
