package net.openid.conformance.condition.rs;

import net.openid.conformance.condition.as.ValidateRefreshToken;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class RequireMtlsRefreshToken extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(ValidateRefreshToken.class);
	}
}
