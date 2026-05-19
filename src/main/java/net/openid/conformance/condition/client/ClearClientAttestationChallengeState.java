package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Resets the per-test Client Attestation challenge state — both the cached
 * {@code vci.attestation_challenge} value and the sticky
 * {@link ExtractClientAttestationChallengeFromResponseHeader#CHALLENGE_ISSUED_BY_SERVER_FLAG}
 * — so a freshly-set-up client (e.g. the second client in a multi-client test) does not inherit
 * the previous client's challenge or trip
 * {@code EnsureNoUseAttestationChallengeErrorAfterServerIssuedChallenge} when its very first
 * attestation-bearing request legitimately bootstraps via {@code use_attestation_challenge}.
 *
 * <p>Invoke at the start of {@code VCIProfileBehavior#configureClientAttestation} so client1's setup
 * is a no-op (state is unset) and client2's setup begins from a clean slate; if the server advertises
 * a challenge_endpoint the subsequent fetch will re-establish both pieces of state for the new client.
 */
public class ClearClientAttestationChallengeState extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		boolean hadFlag = "true".equals(env.getString(
			ExtractClientAttestationChallengeFromResponseHeader.CHALLENGE_ISSUED_BY_SERVER_FLAG));
		env.removeNativeValue(
			ExtractClientAttestationChallengeFromResponseHeader.CHALLENGE_ISSUED_BY_SERVER_FLAG);

		JsonObject vci = env.getObject("vci");
		boolean hadChallenge = vci != null && vci.has("attestation_challenge");
		if (vci != null) {
			vci.remove("attestation_challenge");
		}

		logSuccess("Cleared Client Attestation challenge state",
			args("cleared_server_issued_flag", hadFlag, "cleared_attestation_challenge", hadChallenge));
		return env;
	}
}
