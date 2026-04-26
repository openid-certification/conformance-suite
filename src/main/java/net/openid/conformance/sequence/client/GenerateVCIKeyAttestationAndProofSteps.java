package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.vci10issuer.condition.VCIGenerateAttestationProof;
import net.openid.conformance.vci10issuer.condition.VCIGenerateJwtProof;
import net.openid.conformance.vci10issuer.condition.VCIGenerateKeyAttestationIfNecessary;

public class GenerateVCIKeyAttestationAndProofSteps extends AbstractConditionSequence {

	private final String proofTypeKey;

	public GenerateVCIKeyAttestationAndProofSteps(String proofTypeKey) {
		this.proofTypeKey = proofTypeKey;
	}

	@Override
	public void evaluate() {
		callAndContinueOnFailure(VCIGenerateKeyAttestationIfNecessary.class, ConditionResult.FAILURE,
			"HAIPA-D.1", "OID4VCI-1FINALA-D.1");

		if ("jwt".equals(proofTypeKey)) {
			callAndStopOnFailure(VCIGenerateJwtProof.class, "OID4VCI-1FINALA-F.1");
		} else if ("attestation".equals(proofTypeKey)) {
			callAndStopOnFailure(VCIGenerateAttestationProof.class, "OID4VCI-1FINALA-F.3");
		}
	}
}
