package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractValidateResponseCacheHeaders;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureBackChannelLogoutEndpointResponseContainsCacheHeaders extends AbstractValidateResponseCacheHeaders {

	@Override
	@PreEnvironment( required = "backchannel_logout_endpoint_response" )
	public Environment evaluate(Environment env) {
		JsonObject headers = env.getElementFromObject("backchannel_logout_endpoint_response", "headers").getAsJsonObject();
		String humanReadableResponseName = "RP backchannel_logout_uri response";

		validateCacheHeaders(headers, humanReadableResponseName);

		return env;
	}

}
