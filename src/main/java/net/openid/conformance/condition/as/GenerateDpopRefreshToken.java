package net.openid.conformance.condition.as;

import net.openid.conformance.sequence.AbstractConditionSequence;

public class GenerateDpopRefreshToken extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(CreateDpopRefreshTokenClaims.class);
		callAndStopOnFailure(SetDpopRefreshTokenCnfJkt.class, "DPOP-6.1" );
		callAndStopOnFailure(SignDpopRefreshToken.class);
	}

}
