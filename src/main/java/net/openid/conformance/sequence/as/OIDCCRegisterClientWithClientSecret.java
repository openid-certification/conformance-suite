package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.dynregistration.OIDCCRegisterClient;
import net.openid.conformance.condition.as.dynregistration.OIDCCCreateClientSecretForDynamicClient;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCRegisterClientWithClientSecret extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(OIDCCRegisterClient.class);
		callAndStopOnFailure(OIDCCCreateClientSecretForDynamicClient.class);
	}
}
