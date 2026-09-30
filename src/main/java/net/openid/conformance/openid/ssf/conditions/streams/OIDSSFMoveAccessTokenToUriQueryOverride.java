package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Moves the transmitter access token out of the Authorization header and into the
 * {@code access_token} URI query parameter (the RFC 6750 section 2.3 mechanism), so a test can
 * verify that the transmitter refuses it: CAEP Interop Profile 2.7.2 says a transmitter
 * "MUST NOT accept access tokens via the URI query parameter mechanism".
 * <p>
 * The Authorization header is suppressed entirely so the query parameter is the only
 * credential in the request (RFC 6750 section 2 forbids a client from using more than one
 * method to transmit the token). The stale response of any previous endpoint call is cleared
 * so the subsequent response checks cannot accidentally evaluate it if the call itself fails.
 */
public class OIDSSFMoveAccessTokenToUriQueryOverride extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("access_token", "value");
		if (accessToken == null || accessToken.isBlank()) {
			throw error("No access token available to move into the URI query");
		}

		env.putString("ssf", "access_token_query_override", accessToken);
		env.putString("ssf", "omit_authorization_header", "true");
		env.removeObject("resource_endpoint_response_full");

		logSuccess("Sending the access token as an 'access_token' URI query parameter instead of in the Authorization header",
			args("access_token", accessToken));

		return env;
	}

	public static void undo(Environment env) {
		env.removeElement("ssf", "access_token_query_override");
		env.removeElement("ssf", "omit_authorization_header");
	}
}
