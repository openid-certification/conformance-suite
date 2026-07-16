package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the authorization endpoint response does not contain a vp_token, for use
 * in tests where the wallet is expected to return an error response. This also catches
 * a wallet that returns a vp_token alongside an 'error' parameter — per OID4VP 1.0
 * section 6.4.2 a wallet that cannot deliver all non-optional Credentials must not
 * return any Credential(s).
 */
public class EnsureNoVpTokenInAuthorizationEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement vpToken = env.getElementFromObject("authorization_endpoint_response", "vp_token");

		if (vpToken != null) {
			throw error("The response contains a vp_token, but the wallet was expected to return an error response - a vp_token (whether empty or containing only the satisfiable credentials) must not be returned.",
				args("vp_token", vpToken));
		}

		logSuccess("Authorization endpoint response does not contain a vp_token, as expected.");

		return env;
	}
}
