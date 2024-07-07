package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateEntityStatementBasicClaimsSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(ValidateEntityStatementIss.class, Condition.ConditionResult.FAILURE, "OIDFED-?"); // Spec doesn't explicitly say so?
		callAndContinueOnFailure(ValidateEntityStatementSub.class, Condition.ConditionResult.FAILURE, "OIDFED-?"); // Spec doesn't explicitly say so?
		callAndContinueOnFailure(ValidateEntityStatementIat.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateEntityStatementExp.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		callAndContinueOnFailure(ExtractJWKsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(VerifyEntityStatmentSignature.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
	}
}
