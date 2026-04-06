package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Replaces the client_id in the authorization request with one that uses an
 * invalid/unrecognized client_id prefix scheme.
 *
 * Per OID4VP, wallets must reject requests with unrecognized client_id_prefix values.
 */
public class AddInvalidClientIdPrefixToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	public Environment evaluate(Environment env) {

		JsonObject request = env.getObject("authorization_endpoint_request");

		String originalClientId = OIDFJSON.getString(request.get("client_id"));
		// Strip any existing prefix and add an invalid one
		String bareClientId = originalClientId.contains(":") ?
			originalClientId.substring(originalClientId.indexOf(':') + 1) : originalClientId;
		String invalidClientId = "invalid_scheme:" + bareClientId;

		request.addProperty("client_id", invalidClientId);

		log("Replaced client_id with invalid prefix scheme",
			args("original_client_id", originalClientId, "invalid_client_id", invalidClientId));

		return env;
	}
}
