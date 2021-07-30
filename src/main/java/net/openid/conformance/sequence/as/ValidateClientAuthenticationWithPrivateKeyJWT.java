package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.EnsureClientAssertionSignatureAlgorithmMatchesRegistered;
import net.openid.conformance.condition.as.EnsureClientAssertionTypeIsJwt;
import net.openid.conformance.condition.as.ExtractClientAssertion;
import net.openid.conformance.condition.as.ValidateClientAssertionClaims;
import net.openid.conformance.condition.as.ValidateClientAssertionSignature;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateClientAuthenticationWithPrivateKeyJWT extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		callAndContinueOnFailure(ExtractClientAssertion.class, Condition.ConditionResult.FAILURE, "RFC7523-2.2");
		callAndContinueOnFailure(EnsureClientAssertionSignatureAlgorithmMatchesRegistered.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		callAndContinueOnFailure(ValidateClientAssertionSignature.class, Condition.ConditionResult.FAILURE, "OIDCC-9");
		callAndContinueOnFailure(EnsureClientAssertionTypeIsJwt.class, Condition.ConditionResult.FAILURE, "RFC7523-2.2");
		callAndContinueOnFailure(ValidateClientAssertionClaims.class, Condition.ConditionResult.FAILURE, "RFC7523-3", "OIDCC-9");

	}
}
