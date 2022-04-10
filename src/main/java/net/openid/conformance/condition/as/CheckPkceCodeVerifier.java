package net.openid.conformance.condition.as;

import net.openid.conformance.condition.client.EnsureMinimumPkceCodeVerifierEntropy;
import net.openid.conformance.condition.client.EnsureMinimumPkceCodeVerifierLength;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CheckPkceCodeVerifier extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(ValidateCodeVerifierWithS256.class, "RFC7636-4.6");
		callAndContinueOnFailure(EnsureMinimumPkceCodeVerifierEntropy.class, "RFC7636-7.1");
		callAndContinueOnFailure(EnsureMinimumPkceCodeVerifierLength.class, "RFC7636-7.1");
	}
}
