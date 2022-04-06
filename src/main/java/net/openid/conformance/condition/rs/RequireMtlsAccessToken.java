package net.openid.conformance.condition.rs;

import net.openid.conformance.sequence.AbstractConditionSequence;

public class RequireMtlsAccessToken extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(RequireBearerAccessToken.class);
	}
}
