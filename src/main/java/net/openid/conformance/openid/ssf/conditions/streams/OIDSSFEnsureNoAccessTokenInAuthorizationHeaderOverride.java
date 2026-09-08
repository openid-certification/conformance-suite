package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Removes the access token so the next transmitter request carries no Authorization header
 * at all. SSF 1.0 (Tables 1-10) requires a transmitter to answer such a request with 401,
 * and the CAEP Interop Profile (2.7.2) requires RFC 6750 section 3.1 errors.
 */
public class OIDSSFEnsureNoAccessTokenInAuthorizationHeaderOverride extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		env.putString("no_access_token", "value", "");
		env.putString("no_access_token", "type", "Bearer");
		env.mapKey("access_token", "no_access_token");

		logSuccess("Sending the request without an access token");

		return env;
	}

	public static void undo(Environment env) {
		env.unmapKey("access_token");
		env.removeObject("no_access_token");
	}
}
