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
 */
public class ExtractClientAttestationChallengeFromResponseHeader extends AbstractCondition {

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
		logSuccess("Harvested OAuth-Client-Attestation-Challenge response header for use in the next request",
			args("OAuth-Client-Attestation-Challenge", challenge));
		return env;
	}
}
