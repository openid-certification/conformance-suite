package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Grades a receiver that sent its access token as a URI query parameter, as recorded by
 * {@link OIDSSFHandleAuthorizationHeader} in {@code ssf.auth_result.access_token_in_query}.
 * RFC 6750 2.3 says the query parameter method SHOULD NOT be used; CAEP Interop Profile 2.7.2
 * forbids a transmitter to accept it, so a receiver relying on it cannot interoperate with a
 * conforming transmitter. The caller picks the severity per profile.
 */
public class OIDSSFEnsureReceiverDidNotSendAccessTokenInUriQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		Boolean inQuery = env.getBoolean("ssf", "auth_result.access_token_in_query");
		if (inQuery != null && inQuery) {
			throw error("The receiver sent the access token as a URI query parameter; bearer tokens belong in the Authorization header, "
					+ "and a transmitter conforming to the CAEP Interop Profile must not accept them from the query",
				args("auth_result", env.getElementFromObject("ssf", "auth_result")));
		}

		logSuccess("The receiver sent the access token in the Authorization header");
		return env;
	}
}
