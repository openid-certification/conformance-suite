package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.Condition;

public class OIDSSFUpdateStreamConditionSequence extends OIDSSFCreateStreamConditionSequence{

	@Override
	public void evaluate() {
		super.evaluate();
		replace(OIDSSFCreateStreamConfigCall.class,
			condition(OIDSSFUpdateStreamConfigCall.class)
				.requirements("OIDSSF-7.1.1.3", "CAEPIOP-2.3.8.2")
				.onFail(Condition.ConditionResult.FAILURE));
	}
}
