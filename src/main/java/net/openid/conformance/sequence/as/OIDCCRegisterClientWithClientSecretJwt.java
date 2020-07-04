package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.dynregistration.EnsureTokenEndPointAuthMethodIsClientSecretJwt;

public class OIDCCRegisterClientWithClientSecretJwt extends OIDCCRegisterClientWithClientSecret {

	@Override
	public void evaluate() {
		super.evaluate();
		callAndStopOnFailure(EnsureTokenEndPointAuthMethodIsClientSecretJwt.class);
	}
}
