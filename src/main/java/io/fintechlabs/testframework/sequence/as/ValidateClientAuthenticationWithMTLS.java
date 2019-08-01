package io.fintechlabs.testframework.sequence.as;

import io.fintechlabs.testframework.condition.as.EnsureNoClientAssertionSentToTokenEndpoint;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

public class ValidateClientAuthenticationWithMTLS extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		// The presented TLS certificate has already been verified so nothing to do here.

		callAndStopOnFailure(EnsureNoClientAssertionSentToTokenEndpoint.class);
	}
}
