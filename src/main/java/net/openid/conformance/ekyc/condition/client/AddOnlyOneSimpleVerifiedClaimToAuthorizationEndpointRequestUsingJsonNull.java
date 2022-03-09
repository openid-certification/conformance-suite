package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequestUsingJsonNull extends AbstractAddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequest {
	@Override
	protected JsonElement getClaimValue() {
		return JsonNull.INSTANCE;
	}

}
