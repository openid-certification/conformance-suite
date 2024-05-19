package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddInvalidDpopJktToAuthorizationEndpointRequest extends AbstractCondition {

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
		Base64URL base64urlDpopJkt = new Base64URL(OIDFJSON.getString(dpopKeyObject.get("kid")));
		byte[] bytes = base64urlDpopJkt.decode();
		//Flip some of the bits in the signature to make it invalid
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] ^= 0x5A;
		}
		Base64URL invalidDpopJkt = Base64URL.encode(bytes);
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("dpop_jkt", invalidDpopJkt.toString());

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added invalid dpop_jkt parameter to request", authorizationEndpointRequest);

		return env;
	}

}
