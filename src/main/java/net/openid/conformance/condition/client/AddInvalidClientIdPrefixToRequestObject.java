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
 * Not usable for multi-signed requests, where client_id is absent from the shared payload; see
 * {@link AddInvalidClientIdPrefixToMultiSignedClientIds}.
 *
 * Per OID4VP section 5.9.2, a wallet receiving an unrecognized prefix must either refuse the
 * request or treat the full client_id as referring to a pre-registered client; the value used
 * here is not pre-registered with any wallet, so a conformant wallet cannot complete the flow
 * in either case.
 */
public class AddInvalidClientIdPrefixToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	public Environment evaluate(Environment env) {

		JsonObject request = env.getObject("authorization_endpoint_request");

		String originalClientId = OIDFJSON.getString(request.get("client_id"));
		String invalidClientId = withInvalidPrefix(originalClientId);

		request.addProperty("client_id", invalidClientId);

		log("Replaced client_id with invalid prefix scheme",
			args("original_client_id", originalClientId, "invalid_client_id", invalidClientId));

		return env;
	}

	/** Strips any existing prefix from {@code clientId} and adds an unrecognised one. */
	static String withInvalidPrefix(String clientId) {
		String bareClientId = clientId.contains(":") ?
			clientId.substring(clientId.indexOf(':') + 1) : clientId;
		return "invalid_scheme:" + bareClientId;
	}
}
