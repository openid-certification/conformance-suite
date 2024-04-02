package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.ValidateDpopProofIat;
import net.openid.conformance.condition.as.ValidateDpopProofNbf;
import net.openid.conformance.condition.as.ValidateDpopProofResourceRequest;
import net.openid.conformance.condition.as.ValidateDpopProofSignature;
import net.openid.conformance.condition.as.ValidateResourceEndpointDpopProofNonce;
import net.openid.conformance.condition.client.EnsureDpopProofJtiNotAlreadyUsed;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class PerformDpopProofResourceRequestChecks extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(ValidateDpopProofResourceRequest.class, Condition.ConditionResult.FAILURE, "DPOP-4.3");
		callAndContinueOnFailure(ValidateDpopProofIat.class, Condition.ConditionResult.WARNING, "DPOP-11.1", "FAPI2-SP-ID2-5.3.2.1-14");
		callAndContinueOnFailure(ValidateDpopProofNbf.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.3.2.1-14");
		callAndContinueOnFailure(EnsureDpopProofJtiNotAlreadyUsed.class, Condition.ConditionResult.FAILURE, "DPOP-4.2", "DPOP-11");
		callAndContinueOnFailure(ValidateResourceEndpointDpopProofNonce.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ValidateDpopProofSignature.class, Condition.ConditionResult.FAILURE, "DPOP-4.3-6");
	}
}
