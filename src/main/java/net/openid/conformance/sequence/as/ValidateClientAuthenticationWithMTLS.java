package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.EnsureNoClientAssertionSentToTokenEndpoint;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateClientAuthenticationWithMTLS extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		// The presented TLS certificate has already been verified so nothing to do here.

		callAndStopOnFailure(EnsureNoClientAssertionSentToTokenEndpoint.class);
	}
}
