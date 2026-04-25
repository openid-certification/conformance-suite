package net.openid.conformance.sequence.client;

import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.vci10issuer.condition.SerializeVCICredentialRequestObject;
import net.openid.conformance.vci10issuer.condition.VCIAddCredentialResponseEncryptionToRequest;
import net.openid.conformance.vci10issuer.condition.VCICreateCredentialRequest;
import net.openid.conformance.vci10issuer.condition.VCIEncryptCredentialRequest;

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
		}

		callAndStopOnFailure(SerializeVCICredentialRequestObject.class, "OID4VCI-1FINAL-8.2");

		if (encrypted) {
			callAndStopOnFailure(VCIEncryptCredentialRequest.class, "OID4VCI-1FINAL-8.2", "OID4VCI-1FINAL-10");
		}
	}
}
