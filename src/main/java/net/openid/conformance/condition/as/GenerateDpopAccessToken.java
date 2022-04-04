package net.openid.conformance.condition.as;

import net.openid.conformance.sequence.AbstractConditionSequence;

public class GenerateDpopAccessToken extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(CreateDpopAccessTokenClaims.class);
		callAndStopOnFailure(SetDpopAccessTokenCnfJkt.class, "DPOP-6.1" );
		callAndStopOnFailure(SignDpopAccessToken.class);
	}

}
