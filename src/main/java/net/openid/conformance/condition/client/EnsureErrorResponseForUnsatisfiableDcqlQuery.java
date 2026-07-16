package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the wallet returned an error response for a DCQL query it cannot satisfy.
 *
 * Per OID4VP 1.0 section 6.4.2, a wallet that cannot deliver all non-optional Credentials
 * requested by the Verifier must not return any Credential(s); an empty vp_token object or
 * a partial vp_token cannot be used to signify an error, so the wallet must return an error
 * response instead (see also OID4VP issue #743).
 *
 * This condition only checks that an 'error' parameter is present; the absence of a
 * vp_token (including alongside an 'error' parameter) is checked separately by
 * EnsureNoVpTokenInAuthorizationEndpointResponse.
 */
public class EnsureErrorResponseForUnsatisfiableDcqlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject response = env.getObject("authorization_endpoint_response");

		if (!response.has("error")) {
			throw error("The DCQL query contains a required Credential Query the wallet cannot satisfy, so the wallet must return an error response, but the response does not contain an 'error' parameter.",
				args("authorization_endpoint_response", response));
		}

		logSuccess("Wallet returned an error response for the unsatisfiable DCQL query", response);

		return env;
	}
}
