package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public class AddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequestUsingJsonNull extends AbstractAddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequest {
	@Override
	protected JsonElement getClaimValue() {
		return JsonNull.INSTANCE;
	}

}
