package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Checks if the requested redirect_uri is ONE of the redirect_uris
 * Typically used for dynamically registered clients which may have multiple redirect_uris
 * TODO implement the following checks
 * 3.1.2.1: When using this flow, the Redirection URI SHOULD use the
 * 			https scheme; however, it MAY use the http scheme, provided that the Client Type is confidential,
 * 			as defined in Section 2.1 of OAuth 2.0, and provided the OP allows the use of http Redirection URIs
 * 			in this case. The Redirection URI MAY use an alternate scheme, such as one that is intended to identify
 * 			a callback into a native application.
 * 3.2.2.1: When using this flow, the Redirection URI MUST NOT use the http scheme unless the Client is a native
 * 			application, in which case it MAY use the http: scheme with localhost as the hostname.
 * also note 7.3.  Self-Issued OpenID Provider Request ...Since the Client's redirect_uri URI value is communicated as the Client ID, a redirect_uri parameter is NOT REQUIRED to also be included in the request...
 */
public class EnsureValidRedirectUriForAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", CreateEffectiveAuthorizationRequestParameters.ENV_KEY})
	@PostEnvironment(strings = {"authorization_endpoint_request_redirect_uri"})
	public Environment evaluate(Environment env) {

		JsonElement redirectUrisElement = env.getElementFromObject("client", "redirect_uris");
		if(redirectUrisElement==null) {
			throw error("redirect_uris is undefined for the client");
		}
		try {
			String actual = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.REDIRECT_URI);
			JsonArray redirectUris = redirectUrisElement.getAsJsonArray();
			for(JsonElement e : redirectUris) {
				String uri = OIDFJSON.getString(e);
				if(actual.equals(uri)) {
					logSuccess("redirect_uri is one of the allowed redirect uris",
								args("actual", actual, "expected", redirectUris));
					env.putString("authorization_endpoint_request_redirect_uri", actual);
					return env;
				}
			}
			throw error("redirect_uri is not one of the allowed ones", args("actual", actual, "expected", redirectUris));
		} catch (IllegalStateException ex) {
			throw error("redirect_uris is not an array", ex);
		}
	}

}
