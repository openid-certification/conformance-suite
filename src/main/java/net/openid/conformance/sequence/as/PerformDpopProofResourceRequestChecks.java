package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.ValidateDpopProofResourceRequest;
import net.openid.conformance.condition.as.ValidateDpopProofSignature;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class PerformDpopProofResourceRequestChecks extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(ValidateDpopProofResourceRequest.class, Condition.ConditionResult.FAILURE, "DPOP-4.3");
		callAndContinueOnFailure(ValidateDpopProofSignature.class, Condition.ConditionResult.FAILURE, "DPOP-4.3-6");
	}
}
