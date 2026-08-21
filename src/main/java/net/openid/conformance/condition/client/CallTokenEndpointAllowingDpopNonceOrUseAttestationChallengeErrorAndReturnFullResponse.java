package net.openid.conformance.condition.client;

/**
 * Variant of {@link CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse} for flows that combine
 * DPoP sender constraining with OAuth 2.0 Attestation-Based Client Authentication: in addition to
 * {@code use_dpop_nonce}, a 400 {@code use_attestation_challenge} response
 * (draft-ietf-oauth-attestation-based-client-auth-07 §6.2) is flagged as retryable via
 * {@code token_endpoint_use_attestation_challenge_error}, so the caller can regenerate the client
 * attestation PoP with the freshly returned {@code OAuth-Client-Attestation-Challenge} and retry.
 *
 * <p>Only use this when the client authentication type is client_attestation — for any other auth type
 * the error is invalid and the plain DPoP-nonce wrapper must be used so the 400 fails the test.
 */
public class CallTokenEndpointAllowingDpopNonceOrUseAttestationChallengeErrorAndReturnFullResponse
	extends CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse {

	@Override
	protected boolean recognizeUseAttestationChallengeError() {
		return true;
	}
}
