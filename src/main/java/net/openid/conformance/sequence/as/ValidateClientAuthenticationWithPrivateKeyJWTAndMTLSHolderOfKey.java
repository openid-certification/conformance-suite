package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.EnsureClientAssertionTypeIsJwt;
import net.openid.conformance.condition.as.ExtractClientAssertion;
import net.openid.conformance.condition.as.ValidateClientAssertionClaims;
import net.openid.conformance.condition.as.ValidateClientSigningKeySize;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateClientAuthenticationWithPrivateKeyJWTAndMTLSHolderOfKey extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		callAndStopOnFailure(ExtractClientAssertion.class, "RFC7523-2.2");

		callAndStopOnFailure(EnsureClientAssertionTypeIsJwt.class, "RFC7523-2.2");

		callAndStopOnFailure(ValidateClientAssertionClaims.class, "RFC7523-3");

		callAndStopOnFailure(ValidateClientSigningKeySize.class,"FAPI-R-5.2.2.5");
	}
}
