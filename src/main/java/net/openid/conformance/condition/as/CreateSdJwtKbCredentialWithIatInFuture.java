package net.openid.conformance.condition.as;

import com.nimbusds.jose.jwk.ECKey;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CreateSdJwtKbCredentialWithIatInFuture extends CreateSdJwtKbCredential {

	@Override
	public String keyBindingJwt(ECKey privateKey, String aud, String nonce, String sdHash) {
		long futureIat = Instant.now().plus(365, ChronoUnit.DAYS).getEpochSecond();
		log("Using iat 1 year in the future for key binding JWT",
			args("iat", futureIat));
		return keyBindingJwtWithIat(privateKey, aud, nonce, sdHash, futureIat);
	}
}
