package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Checks if the requested redirect_uri is ONE of the redirect_uris
 * Typically used for dynamically registered clients which may have multiple redirect_uris
 */
public class EnsureValidRedirectUriForAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {
		// get the client ID from the configuration
		JsonElement redirectUrisElement = env.getElementFromObject("client", "redirect_uris");
		if(redirectUrisElement==null) {
			throw error("redirect_uris is undefined for the client");
		}
		try {
			String actual = env.getString("authorization_endpoint_request", "params.redirect_uri");
			JsonArray redirectUris = redirectUrisElement.getAsJsonArray();
			for(int i=0;i<redirectUris.size();i++) {
				String uri = OIDFJSON.getString(redirectUris.get(i));
				if(actual.equals(uri)) {
					logSuccess("redirect_uri is one of the allowed redirect uris");
					return env;
				}
			}
			throw error("redirect_uri is not one of the allowed ones", args("actual", actual, "expected", redirectUris));
		} catch (IllegalStateException ex) {
			throw error("redirect_uris is not an array", ex);
		}
	}

}
