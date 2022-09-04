package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.dynregistration.SetClientIdTokenSignedResponseAlgToHS256;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCRegisterClientWithIdTokenSignedResponseAlgHS256 extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(SetClientIdTokenSignedResponseAlgToHS256.class);
	}
}
