package net.openid.conformance.condition.as;

import net.openid.conformance.sequence.AbstractConditionSequence;

public class GenerateMtlsRefreshToken extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateRefreshToken.class);
	}

}
