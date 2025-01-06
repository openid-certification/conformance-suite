package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.Condition;

public class OIDSSFCreateStreamConditionSequenceWithInvalidAccessToken extends OIDSSFCreateStreamConditionSequence{

	@Override
	public void evaluate() {
		callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
		super.evaluate();
		replace(OIDSSFCreateStreamConfigCall.class, condition(OIDSSFCreateStreamConfigCall.class)
			.requirement("OIDSSF-7.1.1.1")
			.onFail(Condition.ConditionResult.FAILURE));
	}
}
