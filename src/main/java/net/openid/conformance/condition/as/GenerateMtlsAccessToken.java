package net.openid.conformance.condition.as;

import net.openid.conformance.sequence.AbstractConditionSequence;

public class GenerateMtlsAccessToken extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(GenerateBearerAccessToken.class);
	}

}
