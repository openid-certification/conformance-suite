package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Checks that the error returned from the authorization endpoint is 'access_denied',
 * the error code OID4VP 1.0 section 8.5 defines for "The Wallet did not have the
 * requested Credentials to satisfy the Authorization Request".
 */
public class EnsureAuthorizationEndpointErrorIsAccessDenied extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement errorEl = env.getElementFromObject("authorization_endpoint_response", "error");
		if (errorEl == null) {
			throw error("No 'error' parameter in the authorization endpoint response; 'access_denied' was expected.",
				args("authorization_endpoint_response", env.getObject("authorization_endpoint_response")));
		}

		String error = OIDFJSON.getString(errorEl);
		if (!"access_denied".equals(error)) {
			throw error("'access_denied' is the error code defined for a wallet that does not have the requested credentials, but a different error was returned.",
				args("expected", "access_denied", "actual", error));
		}

		logSuccess("Authorization endpoint error is 'access_denied'");

		return env;
	}
}
