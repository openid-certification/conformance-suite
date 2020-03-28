package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.EnsureClientAssertionSignatureAlgorithmMatchesRegistered;
import net.openid.conformance.condition.as.EnsureClientAssertionTypeIsJwt;
import net.openid.conformance.condition.as.ExtractClientAssertion;
import net.openid.conformance.condition.as.ValidateClientAssertionClaims;
import net.openid.conformance.condition.as.ValidateClientAssertionSignature;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateClientAuthenticationWithPrivateKeyJWT extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		callAndStopOnFailure(ExtractClientAssertion.class, "RFC7523-2.2");
		callAndStopOnFailure(EnsureClientAssertionSignatureAlgorithmMatchesRegistered.class, "OIDCR-2");
		callAndStopOnFailure(ValidateClientAssertionSignature.class);
		callAndStopOnFailure(EnsureClientAssertionTypeIsJwt.class, "RFC7523-2.2");
		callAndStopOnFailure(ValidateClientAssertionClaims.class, "RFC7523-3");

	}
}
