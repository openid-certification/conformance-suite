package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.validation.RedirectURIValidationUtil;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Checks if the requested redirect_uri is ONE of the redirect_uris
 * and has the correct scheme
 *
 * also note 7.3.  Self-Issued OpenID Provider Request
 * ...Since the Client's redirect_uri URI value is communicated as the Client ID, a redirect_uri parameter is
 * NOT REQUIRED to also be included in the request...
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
			if (actual == null) {
				throw error("redirect_uri is not present in authorization request", args("auth_request",  env.getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY)));
			}
			try {
				URI uri = new URI(actual);
				if(uri.getFragment()!=null) {
					throw error("Invalid redirect_uri. redirect_uri includes a fragment component.", args("redirect_uri", actual));
				}
			} catch (URISyntaxException e) {
				throw error("Invalid redirect_uri", args("redirect_uri", actual));
			}
			JsonArray redirectUris = redirectUrisElement.getAsJsonArray();
			for(JsonElement e : redirectUris) {
				String uri = OIDFJSON.getString(e);
				if(actual.equals(uri)) {
					//require https if application_type is web and response_type is not equal to code
					String applicationType = env.getString("client", "application_type");
					String responseType = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.RESPONSE_TYPE);
					if(!RedirectURIValidationUtil.requireHttpsIfWebAndResponseTypeNotCode(applicationType, responseType, actual)) {
						throw error("redirect_uri is one of the registered uris but uses http scheme which " +
								"is not allowed when application_type is web and response type is not code",
							args("actual", actual, "expected", redirectUris));
					}
					try {
						if(!RedirectURIValidationUtil.dontAllowHttpIfNativeAndNotLocalhost(applicationType, actual)) {
							throw error("redirect_uri is one of the registered uris but http scheme  " +
									" is only allowed for localhost for native applications",
								args("actual", actual, "expected", redirectUris));
						}
					} catch (URISyntaxException uriSyntaxException) {
						throw error("Invalid redirect_uri syntax", uriSyntaxException, args("actual", actual));
					}
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
