package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.dynregistration.EnsureTokenEndPointAuthMethodIsNone;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCRegisterClientWithNone extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(EnsureTokenEndPointAuthMethodIsNone.class);
	}
}
