package net.openid.conformance.condition.as;

import java.time.Instant;

public class GenerateIdTokenClaimsWith181DayExp extends GenerateIdTokenClaims {

	@Override
	protected Instant getExp(Instant iat) {
		long _181days = 181 * 24 * 60 * 60;
		return iat.plusSeconds(_181days);
	}

}
