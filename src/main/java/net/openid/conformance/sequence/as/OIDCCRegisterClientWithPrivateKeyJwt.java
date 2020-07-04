package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.dynregistration.EnsureTokenEndPointAuthMethodIsPrivateKeyJwt;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCRegisterClientWithPrivateKeyJwt extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		//AbstractOIDCCClientTest will perform jwks validation
		callAndStopOnFailure(EnsureTokenEndPointAuthMethodIsPrivateKeyJwt.class);
	}
}
