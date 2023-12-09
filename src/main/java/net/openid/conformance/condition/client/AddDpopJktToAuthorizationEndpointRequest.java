package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddDpopJktToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "client"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement dpopKeyElement = env.getElementFromObject("client", "dpop_private_jwk");
		if(null == dpopKeyElement) {
			throw error("DPOP key not found");
		}
		JsonObject dpopKeyObject = dpopKeyElement.getAsJsonObject();
		if(!dpopKeyObject.has("kid")) {
			// 'kid' using thumbprint should have been created during key generation
			throw error("DPOP key kid not available");
		}
		String dpop_jkt = OIDFJSON.getString(dpopKeyObject.get("kid"));
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("dpop_jkt", dpop_jkt);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added dpop_jkt parameter to request", authorizationEndpointRequest);

		return env;
	}

}
