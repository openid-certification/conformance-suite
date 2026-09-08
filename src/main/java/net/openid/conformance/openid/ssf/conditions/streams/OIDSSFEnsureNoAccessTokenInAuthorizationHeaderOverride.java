package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Makes the next transmitter request carry no Authorization header at all.
 * SSF 1.0 (Tables 1-10) requires a transmitter to answer such a request with 401,
 * and the CAEP Interop Profile (2.7.2) requires RFC 6750 section 3.1 errors.
 * <p>
 * The stale response of any previous endpoint call is cleared so the subsequent
 * response checks cannot accidentally evaluate it if the call itself fails.
 */
public class OIDSSFEnsureNoAccessTokenInAuthorizationHeaderOverride extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		env.putString("ssf", "omit_authorization_header", "true");
		env.removeObject("resource_endpoint_response_full");

		logSuccess("Sending the request without an Authorization header");

		return env;
	}

	public static void undo(Environment env) {
		env.removeElement("ssf", "omit_authorization_header");
	}
}
