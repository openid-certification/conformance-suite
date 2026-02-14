package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateFederationResponseSignatureSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(ValidateECJWKs.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		callAndContinueOnFailure(EnsureEntityStatementJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE, "RFC7518-6.3.2.1");
		callAndContinueOnFailure(VerifyEntityStatementSignature.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
	}
}
