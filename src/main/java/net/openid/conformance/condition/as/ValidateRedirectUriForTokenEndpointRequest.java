package net.openid.conformance.condition.as;

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
		String actual = env.getString("token_endpoint_request", "body_form_params.redirect_uri");
		JsonElement redirectUrisElement = env.getElementFromObject("client", "redirect_uris");
		if(redirectUrisElement==null) {
			throw error("redirect_uris is undefined for the client. Client configuration or registration request must contain redirect_uris");
		}
		try {
			JsonArray redirectUris = redirectUrisElement.getAsJsonArray();
			for(JsonElement e : redirectUris) {
				String uri = OIDFJSON.getString(e);
				if(actual.equals(uri)) {
					logSuccess("redirect_uri is one of the allowed redirect uris", args("actual", actual, "expected", redirectUris));
					return env;
				}
			}
			throw error("redirect_uri is not one of the allowed ones", args("actual", actual, "expected", redirectUris));
		} catch (IllegalStateException ex) {
			throw error("redirect_uris is not an array", ex, args("redirect_uris", redirectUrisElement));
		}
	}

}
