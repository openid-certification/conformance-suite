package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.dynregistration.SetClientGrantTypesToAuthorizationCodeOnly;
import net.openid.conformance.condition.as.dynregistration.SetClientIdTokenSignedResponseAlgToNone;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCRegisterClientWithIdTokenSignedResponseAlgNone extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(SetClientIdTokenSignedResponseAlgToNone.class);
		callAndStopOnFailure(SetClientGrantTypesToAuthorizationCodeOnly.class);
	}
}
