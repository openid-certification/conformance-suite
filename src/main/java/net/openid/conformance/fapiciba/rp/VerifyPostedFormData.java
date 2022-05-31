package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class VerifyPostedFormData extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(BackchannelRequestIsPosted.class, Condition.ConditionResult.FAILURE, "CIBA-7.1");
		callAndContinueOnFailure(BackchannelRequestIsFormData.class, Condition.ConditionResult.FAILURE, "CIBA-7.1");
	}
}
