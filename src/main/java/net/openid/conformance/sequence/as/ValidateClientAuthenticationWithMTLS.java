package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.EnsureClientIdIsPresentOnTokenRequest;
import net.openid.conformance.condition.as.EnsureNoClientAssertionSentToTokenEndpoint;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateClientAuthenticationWithMTLS extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		// The presented TLS certificate has already been verified so nothing to do here.
		callAndContinueOnFailure(EnsureClientIdIsPresentOnTokenRequest.class, Condition.ConditionResult.FAILURE, "RFC8705-2");

		callAndStopOnFailure(EnsureNoClientAssertionSentToTokenEndpoint.class);
	}
}
