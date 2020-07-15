package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.dynregistration.EnsureTokenEndPointAuthMethodIsClientSecretBasic;

public class OIDCCRegisterClientWithClientSecretBasic extends OIDCCRegisterClientWithClientSecret {

	@Override
	public void evaluate() {
		super.evaluate();
		callAndStopOnFailure(EnsureTokenEndPointAuthMethodIsClientSecretBasic.class);
	}
}
