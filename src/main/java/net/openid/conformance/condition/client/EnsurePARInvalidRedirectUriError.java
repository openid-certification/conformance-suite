package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsurePARInvalidRedirectUriError extends AbstractEnsureSpecifiedErrorFromPushedAuthorizationEndpointResponse {

	public static final String  INVALID_REDIRECT_URI = "invalid_redirect_uri";

	@Override
	protected String getExpectedError() {
		return INVALID_REDIRECT_URI;
	}

}
