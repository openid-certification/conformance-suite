package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class BackchannelValidateClientAuthenticationWithMTLS extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		// The presented TLS certificate has already been verified so nothing to do here.
		callAndContinueOnFailure(BackchannelEnsureClientIdIsPresentOnTokenRequest.class, Condition.ConditionResult.FAILURE, "RFC8705-2");

		callAndStopOnFailure(BackchannelEnsureNoClientAssertionSentToTokenEndpoint.class);
	}
}
