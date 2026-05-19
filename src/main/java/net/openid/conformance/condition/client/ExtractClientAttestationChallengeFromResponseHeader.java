package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Captures the {@code OAuth-Client-Attestation-Challenge} response header from the most recent endpoint response
 * (mapped via {@code endpoint_response}) into {@code vci.attestation_challenge} so the next client-attestation PoP
 * picks up the freshest server-supplied challenge, per
 * draft-ietf-oauth-attestation-based-client-auth-07 §8.1.
 *
 * Always succeeds: a missing header is normal (the AS may rotate per request, or not at all). Validation of the
 * value is deferred to {@link ValidateClientAttestationChallengeResponseHeader}.
 *
 * <p>When a challenge is successfully harvested, also sets the sticky env flag
 * {@link #CHALLENGE_ISSUED_BY_SERVER_FLAG}. Callers can read it to assert that any subsequent
 * {@code use_attestation_challenge} error from the AS represents a contradiction (the AS rejected its own
 * freshly-issued challenge) — see {@code EnsureNoUseAttestationChallengeErrorAfterServerIssuedChallenge}.
 */
public class ExtractClientAttestationChallengeFromResponseHeader extends AbstractCondition {

	/**
	 * Sticky env key set to "true" once any AS-side issuance path (challenge_endpoint or §8.1 response
	 * header harvest) has placed an attestation challenge into env. Once set, a subsequent
	 * {@code use_attestation_challenge} error from the AS is a contradiction — the wallet was using the
	 * fresh challenge the AS just supplied.
	 */
	public static final String CHALLENGE_ISSUED_BY_SERVER_FLAG = "attestation_challenge_issued_by_server";

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement el = env.getElementFromObject("endpoint_response", "headers.oauth-client-attestation-challenge");
		if (el == null) {
			logSuccess("No OAuth-Client-Attestation-Challenge response header to harvest");
			return env;
		}

		String challenge;
		if (el.isJsonArray()) {
			// multi-instance is reported by ValidateClientAttestationChallengeResponseHeader; here we just take the first
			if (el.getAsJsonArray().isEmpty()) {
				logSuccess("No OAuth-Client-Attestation-Challenge response header to harvest");
				return env;
			}
			challenge = OIDFJSON.getString(el.getAsJsonArray().get(0));
		} else {
			challenge = OIDFJSON.getString(el);
		}

		if (challenge.isEmpty()) {
			logSuccess("OAuth-Client-Attestation-Challenge response header was empty; nothing to harvest");
			return env;
		}

		env.putString("vci", "attestation_challenge", challenge);
		env.putString(CHALLENGE_ISSUED_BY_SERVER_FLAG, "true");
		logSuccess("Harvested OAuth-Client-Attestation-Challenge response header for use in the next request",
			args("OAuth-Client-Attestation-Challenge", challenge));
		return env;
	}
}
