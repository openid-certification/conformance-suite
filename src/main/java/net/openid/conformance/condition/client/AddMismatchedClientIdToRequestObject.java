package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Replaces the client_id in the request object claims with a deliberately wrong value,
 * creating a mismatch between the client_id in the URL and the request object.
 *
 * Per OID4VP 1.0 Final section 5: "The Client Identifier value in the client_id
 * Authorization Request parameter and the Request Object client_id claim value
 * MUST be identical, including the Client Identifier Scheme."
 *
 * Wallets MUST reject requests where these values differ.
 */
public class AddMismatchedClientIdToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"request_object_claims"})
	@PostEnvironment(required = {"request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		String originalClientId = OIDFJSON.getString(requestObjectClaims.get("client_id"));
		String mismatchedClientId = originalClientId + "-mismatched";

		requestObjectClaims.addProperty("client_id", mismatchedClientId);

		log("Replaced client_id in request object with a mismatched value",
			args("original_client_id", originalClientId, "mismatched_client_id", mismatchedClientId));

		return env;
	}
}
