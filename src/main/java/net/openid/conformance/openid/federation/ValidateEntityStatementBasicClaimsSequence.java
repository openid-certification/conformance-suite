package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateEntityStatementBasicClaimsSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(ValidateEntityStatementIss.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		callAndContinueOnFailure(ValidateEntityStatementSub.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		callAndContinueOnFailure(ValidateEntityStatementIat.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		callAndContinueOnFailure(ValidateEntityStatementExp.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
	}
}
