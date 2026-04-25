package net.openid.conformance.sequence.client;

import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.vci10issuer.condition.VCIAddCredentialResponseEncryptionToRequest;
import net.openid.conformance.vci10issuer.condition.VCICreateCredentialRequest;

public class CreateVCICredentialRequestSteps extends AbstractConditionSequence {

	private final boolean encrypted;

	public CreateVCICredentialRequestSteps(boolean encrypted) {
		this.encrypted = encrypted;
	}

	@Override
	public void evaluate() {
		callAndStopOnFailure(VCICreateCredentialRequest.class, "OID4VCI-1FINAL-8.2");

		if (encrypted) {
			callAndStopOnFailure(VCIAddCredentialResponseEncryptionToRequest.class, "OID4VCI-1FINAL-8.2");
			afterCredentialResponseEncryptionAdded();
		}
	}

	protected void afterCredentialResponseEncryptionAdded() {
		// Default implementation does nothing.
	}
}
