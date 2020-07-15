package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.dynregistration.EnsureTokenEndPointAuthMethodIsClientSecretPost;

public class OIDCCRegisterClientWithClientSecretPost extends OIDCCRegisterClientWithClientSecret {

	@Override
	public void evaluate() {
		super.evaluate();
		callAndStopOnFailure(EnsureTokenEndPointAuthMethodIsClientSecretPost.class);
	}
}
