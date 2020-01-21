package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Registration request must contain at least one redirect_uri
 */
public class OIDCCValidateDynamicRegistrationRedirectUris extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request"})
	public Environment evaluate(Environment env) {
		JsonObject dynRegRequest = env.getObject("dynamic_registration_request");
		int validUriCount = 0;
		String redirectUriString = null;
		JsonArray redirectUrisArray = null;
		try {
			redirectUrisArray = dynRegRequest.getAsJsonArray("redirect_uris");
			if(redirectUrisArray==null) {
				throw error("redirect_uris is not set");
			}
		} catch (ClassCastException ex) {
			throw error("redirect_uris is not encoded as an array", ex);
		}
		for(int i=0; i<redirectUrisArray.size(); i++) {
			try {
				redirectUriString = OIDFJSON.getString(redirectUrisArray.get(i));
				URI uri = new URI(redirectUriString);
				if(uri.getFragment()!=null) {
					throw error("Invalid redirect uri. URI includes a fragment component.", args("invalid_uri", redirectUriString));
				}
				validUriCount++;
			} catch (URISyntaxException e) {
				throw error("Invalid redirect uri ", e, args("invalid_uri", redirectUriString));
			}
		}
		if(validUriCount==0) {
			throw error("At least one redirect_uri is required in dynamic client registration requests.");
		}
		logSuccess("Valid redirect_uri(s) provided in registration request", args("redirect_uris", redirectUrisArray));
		return env;
	}
}
