package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.dynregistration.EnsureTokenEndPointAuthMethodIsTlsClientAuth;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCRegisterClientWithTlsClientAuth extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(EnsureTokenEndPointAuthMethodIsTlsClientAuth.class);
	}
}
