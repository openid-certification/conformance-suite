package net.openid.conformance.condition.as;

import com.nimbusds.jose.jwk.ECKey;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CreateSdJwtKbCredentialWithIatInPast extends CreateSdJwtKbCredential {

	@Override
	public String keyBindingJwt(ECKey privateKey, String aud, String nonce, String sdHash) {
		long pastIat = Instant.now().minus(365, ChronoUnit.DAYS).getEpochSecond();
		log("Using iat 1 year in the past for key binding JWT",
			args("iat", pastIat));
		return keyBindingJwtWithIat(privateKey, aud, nonce, sdHash, pastIat);
	}
}
