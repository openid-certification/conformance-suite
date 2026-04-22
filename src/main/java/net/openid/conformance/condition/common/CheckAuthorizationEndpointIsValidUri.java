package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;


/**
 * Checks that the authorization_endpoint from the server configuration has a valid URI scheme.
 *
 * Only validates the scheme portion (e.g. "https", "openid4vp") — full URI parsing is not
 * performed because custom schemes like openid4vp:// are not accepted by java.net.URI.
 */
public class CheckAuthorizationEndpointIsValidUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		String authorizationEndpoint = env.getString("server", "authorization_endpoint");
		if (authorizationEndpoint == null || authorizationEndpoint.isBlank()) {
			throw error("authorization_endpoint is missing or empty in the server configuration");
		}

		int colonIndex = authorizationEndpoint.indexOf(':');
		if (colonIndex <= 0) {
			throw error("authorization_endpoint does not have a URI scheme",
				args("authorization_endpoint", authorizationEndpoint));
		}

		String scheme = authorizationEndpoint.substring(0, colonIndex);
		if (!scheme.matches("[a-zA-Z][a-zA-Z0-9+\\-.]*")) {
			throw error("authorization_endpoint has an invalid URI scheme",
				args("authorization_endpoint", authorizationEndpoint, "scheme", scheme));
		}

		logSuccess("authorization_endpoint has a valid URI scheme",
			args("authorization_endpoint", authorizationEndpoint));
		return env;
	}
}
