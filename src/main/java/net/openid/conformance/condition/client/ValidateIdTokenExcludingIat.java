package net.openid.conformance.condition.client;

import java.time.Instant;

public class ValidateIdTokenExcludingIat extends ValidateIdToken {

	@Override
	protected void verifyIat(Instant now, Long iat) {
	}

}
