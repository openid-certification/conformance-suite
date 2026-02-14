package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateFederationResponseSignatureSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(ValidateECJWKs.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInEntityStatementJWKs.class, Condition.ConditionResult.FAILURE, "OIDFED-3.1.1");
		callAndContinueOnFailure(EnsureEntityStatementJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE, "RFC7518-6.3.2.1");
		callAndContinueOnFailure(EnsureEntityStatementJwksHasMinimumKeyLength.class, Condition.ConditionResult.FAILURE, "RFC7518-3.3", "RFC7518-3.4", "RFC7518-3.5");
		callAndContinueOnFailure(VerifyEntityStatementSignature.class, Condition.ConditionResult.FAILURE, "OIDFED-3.2");
	}
}
