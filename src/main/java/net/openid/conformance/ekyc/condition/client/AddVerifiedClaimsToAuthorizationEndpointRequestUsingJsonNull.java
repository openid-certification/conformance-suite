package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public class AddVerifiedClaimsToAuthorizationEndpointRequestUsingJsonNull extends AbstractAddVerifiedClaimsToAuthorizationEndpointRequest {
	@Override
	protected JsonElement getClaimValue() {
		return JsonNull.INSTANCE;
	}

}
