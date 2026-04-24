package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;

/**
 * Validates the client attestation JWT's 'nbf' claim if present. Per OAuth 2.0
 * Attestation-Based Client Authentication §5.1, 'nbf' is OPTIONAL — the condition skips
 * when it is absent. When present, the current time must be at or after nbf (allowing
 * clock skew), and nbf must not be unreasonably far in the future.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07#section-5.1">OAuth 2.0 Attestation-Based Client Authentication §5.1</a>
 */
public class ValidateClientAttestationNotBefore extends AbstractCondition {

	private static final long CLOCK_SKEW_SECONDS = 5 * 60;

	@Override
	@PreEnvironment(required = "client_attestation_object")
	public Environment evaluate(Environment env) {

		JsonElement nbfEl = env.getElementFromObject("client_attestation_object", "claims.nbf");

		if (nbfEl == null) {
			log("Client attestation JWT has no nbf claim (optional), skipping check");
			return env;
		}

		long nbf = OIDFJSON.getLong(nbfEl);
		long now = Instant.now().getEpochSecond();

		if (nbf > now + CLOCK_SKEW_SECONDS) {
			throw error("Client attestation JWT nbf is in the future — attestation not yet valid",
				args("nbf", nbf, "now", now, "clock_skew_seconds", CLOCK_SKEW_SECONDS));
		}

		logSuccess("Client attestation JWT nbf claim is valid", args("nbf", nbf, "now", now));

		return env;
	}
}
