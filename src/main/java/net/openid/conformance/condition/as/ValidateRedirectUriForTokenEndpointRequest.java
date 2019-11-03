package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Typically used for dynamically registered clients
 * TODO extend this one and EnsureValidRedirectUriForAuthorizationEndpointRequest from a common base and probably rename
 */
public class ValidateRedirectUriForTokenEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", "token_endpoint_request" })
	public Environment evaluate(Environment env) {
		String actual = env.getString("token_endpoint_request", "params.redirect_uri");
		JsonElement redirectUrisElement = env.getElementFromObject("client", "redirect_uris");
		if(redirectUrisElement==null) {
			throw error("redirect_uris is undefined for the client");
		}
		try {
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
