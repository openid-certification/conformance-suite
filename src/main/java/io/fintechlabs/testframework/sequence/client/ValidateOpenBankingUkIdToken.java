package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.OBValidateIdTokenIntentId;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

public class ValidateOpenBankingUkIdToken extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(OBValidateIdTokenIntentId.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
	}

}
