package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Adds a client_id using an unknown Client Identifier Prefix to the authorization request.
 *
 * For unsigned requests over the DC API the client_id parameter must be omitted and the wallet
 * MUST ignore any client_id parameter that is present (OID4VP Appendix A.2), so a conformant
 * wallet must complete the flow normally even though the prefix is unknown.
 */
public class AddClientIdWithUnknownPrefixToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject request = env.getObject("authorization_endpoint_request");

		String clientId = "invalid_scheme:example";
		request.addProperty("client_id", clientId);

		log("Added client_id with unknown prefix to authorization request",
			args("client_id", clientId));

		return env;
	}
}
