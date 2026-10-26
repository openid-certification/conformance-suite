package net.openid.conformance.vci10issuer.condition.clientattestation;

import net.openid.conformance.vci10issuer.condition.AbstractCheckChallengeOrNonceResponseForUnknownFields;

import java.util.Set;

/**
 * The specification lets the authorization server add further challenges or data to the challenge
 * response, so unknown fields are permitted; the caller reports them as a warning for sender hygiene.
 */
public class CheckClientAttestationChallengeResponseForUnknownFields extends AbstractCheckChallengeOrNonceResponseForUnknownFields {

	@Override
	protected Set<String> knownFields() {
		return Set.of("attestation_challenge");
	}

	@Override
	protected String sharedEndpointField() {
		return "c_nonce";
	}

	@Override
	protected String responseName() {
		return "Challenge endpoint response";
	}

	@Override
	protected String responseArgName() {
		return "challenge_response";
	}
}
