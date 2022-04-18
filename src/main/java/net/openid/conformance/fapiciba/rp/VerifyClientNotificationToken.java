package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class VerifyClientNotificationToken extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(BackchannelRequestHasClientNotificationToken.class, Condition.ConditionResult.FAILURE,"CIBA-7.1");
		callAndContinueOnFailure(BackchannelRequestClientNotificationTokenLength.class, Condition.ConditionResult.FAILURE,"CIBA-7.1");
		callAndContinueOnFailure(BackchannelRequestClientNotificationTokenEntropy.class, Condition.ConditionResult.FAILURE,"CIBA-7.1");
	}
}
