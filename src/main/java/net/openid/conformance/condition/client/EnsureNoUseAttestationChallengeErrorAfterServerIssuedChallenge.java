package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Fails when the Authorization Server has previously issued a Client Attestation Challenge — either via
 * the challenge endpoint (draft-ietf-oauth-attestation-based-client-auth-07 §8) or via an
 * {@code OAuth-Client-Attestation-Challenge} response header (§8.1) — and a subsequent request to the
 * AS is still rejected with a {@code use_attestation_challenge} error (§6.2). The error code is a
 * signal that the wallet did not include a fresh challenge; if the AS just supplied one and the wallet
 * used it, returning the error is an AS bug that the retry loop would otherwise mask.
 *
 * <p>Reads the sticky flag {@link ExtractClientAttestationChallengeFromResponseHeader#CHALLENGE_ISSUED_BY_SERVER_FLAG},
 * plus the per-endpoint use_attestation_challenge error keys set by the
 * {@code Call*AllowingUseAttestationChallengeError*} wrappers
 * ({@code par_endpoint_use_attestation_challenge_error}, {@code token_endpoint_use_attestation_challenge_error}).
 * Invoke between the Call* condition and the {@code ExtractClientAttestationChallengeFromResponseHeader}
 * harvest so the flag still reflects pre-call state.
 */
public class EnsureNoUseAttestationChallengeErrorAfterServerIssuedChallenge extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String issuedByServer = env.getString(ExtractClientAttestationChallengeFromResponseHeader.CHALLENGE_ISSUED_BY_SERVER_FLAG);
		if (!"true".equals(issuedByServer)) {
			// No AS-issuance path (challenge endpoint or §8.1 header) has fired yet, so a
			// use_attestation_challenge error here is the legitimate initial-bootstrap path.
			log("No server-issued attestation challenge observed yet; use_attestation_challenge is permitted as the initial issuance path");
			return env;
		}
		String parError = env.getString("par_endpoint_use_attestation_challenge_error");
		String tokenError = env.getString("token_endpoint_use_attestation_challenge_error");
		if (Strings.isNullOrEmpty(parError) && Strings.isNullOrEmpty(tokenError)) {
			logSuccess("No use_attestation_challenge error after server-issued attestation challenge");
			return env;
		}
		throw error("""
				Authorization Server returned use_attestation_challenge after already issuing a Client Attestation Challenge to the \
				wallet - the wallet was using the freshly issued value, so this looks like an AS bug.""",
			args("par_endpoint_use_attestation_challenge_error", parError,
				"token_endpoint_use_attestation_challenge_error", tokenError));
	}
}
