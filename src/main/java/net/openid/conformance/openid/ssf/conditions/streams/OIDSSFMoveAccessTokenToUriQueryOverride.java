package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Moves the transmitter access token out of the Authorization header and into the
 * {@code access_token} URI query parameter (the RFC 6750 section 2.3 mechanism), so a test can
 * verify that the transmitter refuses it: CAEP Interop Profile 2.7.2 says a transmitter
 * "MUST NOT accept access tokens via the URI query parameter mechanism".
 * <p>
 * The token value is stashed for {@link #undo(Environment)} and the header token is replaced
 * with an empty value so the request carries no bearer credentials at all.
 */
public class OIDSSFMoveAccessTokenToUriQueryOverride extends AbstractCondition {

	public static final String QUERY_TOKEN_KEY = "ssf_query_access_token";

	@Override
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("access_token", "value");
		if (accessToken == null || accessToken.isBlank()) {
			throw error("No access token available to move into the URI query");
		}

		env.putString(QUERY_TOKEN_KEY, "value", accessToken);
		env.putString("ssf", "access_token_query_override", accessToken);

		logSuccess("Sending the access token as an 'access_token' URI query parameter instead of in the Authorization header",
			args("access_token", accessToken));

		return env;
	}

	public static void undo(Environment env) {
		env.removeObject(QUERY_TOKEN_KEY);
		env.removeElement("ssf", "access_token_query_override");
	}
}
