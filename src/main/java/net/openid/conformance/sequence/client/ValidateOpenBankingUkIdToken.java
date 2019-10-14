package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.OBValidateIdTokenIntentId;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateOpenBankingUkIdToken extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(OBValidateIdTokenIntentId.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
	}

}
