package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequestUsingEmptyJsonObject extends AbstractAddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequest {
	@Override
	protected JsonElement getClaimValue() {
		return new JsonObject();
	}

}
