package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.dynregistration.SetClientIdTokenSignedResponseAlgToES256;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCRegisterClientWithIdTokenSignedResponseAlgES256 extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(SetClientIdTokenSignedResponseAlgToES256.class);
	}
}
