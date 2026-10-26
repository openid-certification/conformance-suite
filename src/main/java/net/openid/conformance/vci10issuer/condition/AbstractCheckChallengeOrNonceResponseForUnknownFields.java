package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

/**
 * Flags fields the specification does not define in a client attestation challenge response or a
 * credential nonce response.
 *
 * <p>A server may serve both endpoints from one URL and answer with both {@code attestation_challenge}
 * and {@code c_nonce}. The field defined for the other endpoint is therefore only reported when the
 * advertised {@code challenge_endpoint} and {@code nonce_endpoint} URLs differ.
 */
public abstract class AbstractCheckChallengeOrNonceResponseForUnknownFields extends AbstractCondition {

	/** Fields the specification defines for this endpoint's response. */
	protected abstract Set<String> knownFields();

	/** The field defined for the other endpoint, accepted when both endpoints share one URL. */
	protected abstract String sharedEndpointField();

	/** Human-readable name of the response, used in log messages. */
	protected abstract String responseName();

	/** Key under which the response body is logged. */
	protected abstract String responseArgName();

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		String responseBody = OIDFJSON.getString(env.getElementFromObject("endpoint_response", "body"));
		JsonObject responseObject = JsonParser.parseString(responseBody).getAsJsonObject();

		Set<String> unknownFields = new HashSet<>(responseObject.keySet());
		unknownFields.removeAll(knownFields());
		if (challengeAndNonceEndpointsAreShared(env)) {
			unknownFields.remove(sharedEndpointField());
		}
		if (!unknownFields.isEmpty()) {
			throw error(responseName() + " contains unknown fields",
				args("unknown_fields", unknownFields, responseArgName(), responseObject));
		}

		logSuccess(responseName() + " contains no unknown fields");
		return env;
	}

	private static boolean challengeAndNonceEndpointsAreShared(Environment env) {
		String challengeEndpoint = env.getString("server", "challenge_endpoint");
		String nonceEndpoint = env.getString("vci", "credential_issuer_metadata.nonce_endpoint");
		return challengeEndpoint != null && challengeEndpoint.equals(nonceEndpoint);
	}
}
