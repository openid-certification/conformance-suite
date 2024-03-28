package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.ValidateAuthorizationCodeDpopBindingKey;
import net.openid.conformance.condition.as.ValidateDpopProofIat;
import net.openid.conformance.condition.as.ValidateDpopProofSignature;
import net.openid.conformance.condition.as.ValidateDpopProofTokenRequest;
import net.openid.conformance.condition.as.ValidateTokenEndpointDpopProofNonce;
import net.openid.conformance.condition.client.EnsureDpopProofJtiNotUsed;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class PerformDpopProofTokenRequestChecks extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(ValidateDpopProofTokenRequest.class, Condition.ConditionResult.FAILURE, "DPOP-4.3");
		callAndContinueOnFailure(ValidateDpopProofIat.class, Condition.ConditionResult.WARNING, "DPOP-11.1", "FAPI2-SP-ID2-5.3.2.1-14");
		callAndContinueOnFailure(EnsureDpopProofJtiNotUsed.class, Condition.ConditionResult.FAILURE, "DPOP-4.2", "DPOP-11");
		callAndContinueOnFailure(ValidateTokenEndpointDpopProofNonce.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ValidateDpopProofSignature.class, Condition.ConditionResult.FAILURE, "DPOP-4.3-6");
		callAndContinueOnFailure(ValidateAuthorizationCodeDpopBindingKey.class, Condition.ConditionResult.FAILURE,"DPOP-10");
	}
}
