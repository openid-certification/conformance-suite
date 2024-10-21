package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
public class EnsureValidResponseUriForAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", CreateEffectiveAuthorizationRequestParameters.ENV_KEY})
	public Environment evaluate(Environment env) {

		String responseUri = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "response_uri");
		URI uri;
		try {
			uri = new URI(responseUri);
		} catch (URISyntaxException e) {
			throw error("response_uri is not a valid URI", e, args("response_uri", responseUri));
		}

		if (uri.getFragment() != null) {
			throw error("Invalid response_uri. response_uri includes a fragment component.", args("response_uri", responseUri));
		}

		String scheme = uri.getScheme();
		if (scheme == null) {
			throw error("response_uri does not have a scheme.", args("response_uri", responseUri));
		}
		if (!scheme.equalsIgnoreCase("https")) {
			throw error("response_uri scheme is not https.", args("response_uri", responseUri));
		}

		logSuccess("response_uri is valid https url with no fragment",
			args("actual", responseUri));
		return env;
	}

}
