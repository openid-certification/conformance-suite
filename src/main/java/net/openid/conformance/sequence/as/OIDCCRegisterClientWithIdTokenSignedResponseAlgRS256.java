package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.dynregistration.SetClientIdTokenSignedResponseAlgToRS256;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCRegisterClientWithIdTokenSignedResponseAlgRS256 extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(SetClientIdTokenSignedResponseAlgToRS256.class);
	}
}
