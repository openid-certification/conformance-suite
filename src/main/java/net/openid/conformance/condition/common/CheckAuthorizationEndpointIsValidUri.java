package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validates that the authorization_endpoint from the server configuration is a valid URI.
 *
 * Uses java.net.URI (not URL) to support custom URI schemes like openid4vp://
 * which are registered per OID4VP Appendix E.8.1 but not recognized by java.net.URL.
 */
public class CheckAuthorizationEndpointIsValidUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		String authorizationEndpoint = env.getString("server", "authorization_endpoint");
		if (authorizationEndpoint == null || authorizationEndpoint.isBlank()) {
			throw error("authorization_endpoint is missing or empty in the server configuration");
		}

		try {
			URI uri = new URI(authorizationEndpoint);
			if (uri.getScheme() == null) {
				throw error("authorization_endpoint does not have a URI scheme",
					args("authorization_endpoint", authorizationEndpoint));
			}
		} catch (URISyntaxException e) {
			throw error("authorization_endpoint is not a valid URI",
				args("authorization_endpoint", authorizationEndpoint, "error", e.getMessage()));
		}

		logSuccess("authorization_endpoint is a valid URI",
			args("authorization_endpoint", authorizationEndpoint));
		return env;
	}
}
