package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.EnsureClientIdIsPresentOnTokenRequest;
import net.openid.conformance.condition.as.EnsureNoClientAssertionSentToTokenEndpoint;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateClientNotificationToken extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(BackchannelRequestHasClientNotificationTokenCondition.class, Condition.ConditionResult.FAILURE,"CIBA-7.1");
		callAndContinueOnFailure(BackchannelRequestClientNotificationTokenLengthCondition.class, Condition.ConditionResult.FAILURE,"CIBA-7.1");
		callAndContinueOnFailure(BackchannelRequestClientNotificationTokenEntropyCondition.class, Condition.ConditionResult.FAILURE,"CIBA-7.1");
	}
}
