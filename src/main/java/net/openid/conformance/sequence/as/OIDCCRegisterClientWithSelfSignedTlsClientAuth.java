package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.dynregistration.EnsureTokenEndPointAuthMethodIsSelfSignedTlsClientAuth;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCRegisterClientWithSelfSignedTlsClientAuth extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(EnsureTokenEndPointAuthMethodIsSelfSignedTlsClientAuth.class);
	}
}
