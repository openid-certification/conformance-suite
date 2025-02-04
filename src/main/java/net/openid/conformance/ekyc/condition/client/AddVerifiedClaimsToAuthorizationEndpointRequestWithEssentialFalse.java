package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AddVerifiedClaimsToAuthorizationEndpointRequestWithEssentialFalse extends AbstractAddVerifiedClaimsToAuthorizationEndpointRequest {
	@Override
	protected JsonElement getClaimValue() {
		JsonObject o = new JsonObject();
		o.addProperty("essential", false);
		return o;
	}

}
