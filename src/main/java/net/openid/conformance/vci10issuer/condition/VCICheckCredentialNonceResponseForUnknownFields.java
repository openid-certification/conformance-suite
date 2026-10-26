package net.openid.conformance.vci10issuer.condition;

import java.util.Set;

public class VCICheckCredentialNonceResponseForUnknownFields extends AbstractCheckChallengeOrNonceResponseForUnknownFields {

	@Override
	protected Set<String> knownFields() {
		return Set.of("c_nonce");
	}

	@Override
	protected String sharedEndpointField() {
		return "attestation_challenge";
	}

	@Override
	protected String responseName() {
		return "Nonce endpoint response";
	}

	@Override
	protected String responseArgName() {
		return "nonce_response";
	}
}
