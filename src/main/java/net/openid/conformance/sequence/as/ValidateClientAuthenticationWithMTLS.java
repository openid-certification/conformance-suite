package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CheckClientIdMatchesOnTokenRequestIfPresent;
import net.openid.conformance.condition.as.EnsureClientIdIsPresentOnTokenRequest;
import net.openid.conformance.condition.as.EnsureNoClientAssertionSentToTokenEndpoint;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateClientAuthenticationWithMTLS extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		callAndStopOnFailure(EnsureClientIdIsPresentOnTokenRequest.class, Condition.ConditionResult.FAILURE, "RFC8705-2");
		callAndStopOnFailure(CheckClientIdMatchesOnTokenRequestIfPresent.class, Condition.ConditionResult.FAILURE, "RFC6749-3.2.1");

		// The presented TLS certificate has already been verified so nothing to do here.

		callAndStopOnFailure(EnsureNoClientAssertionSentToTokenEndpoint.class);
	}
}
