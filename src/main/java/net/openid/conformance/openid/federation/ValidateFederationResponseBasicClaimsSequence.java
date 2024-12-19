package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateFederationResponseBasicClaimsSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		call(condition(ValidateEntityStatementIss.class)
			.skipIfStringsMissing("federation_response_iss")
			.onSkip(Condition.ConditionResult.FAILURE)
			.requirements("OIDFED-3")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());

		call(condition(ValidateEntityStatementSub.class)
			.skipIfStringsMissing("federation_response_sub")
			.onSkip(Condition.ConditionResult.FAILURE)
			.requirements("OIDFED-3")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());

		call(condition(ValidateEntityStatementIat.class)
			.skipIfLongsMissing("federation_response_iat")
			.onSkip(Condition.ConditionResult.FAILURE)
			.requirements("OIDFED-3")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());

		call(condition(ValidateEntityStatementExp.class)
			.skipIfLongsMissing("federation_response_exp")
			.onSkip(Condition.ConditionResult.FAILURE)
			.requirements("OIDFED-3")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());

	}
}
