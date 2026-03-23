package net.openid.conformance.condition.as;

import com.nimbusds.jose.jwk.ECKey;

public class CreateSdJwtKbCredentialWithInvalidAud extends CreateSdJwtKbCredential {

	@Override
	public String keyBindingJwt(ECKey privateKey, String aud, String nonce, String sdHash) {
		String invalidAud = aud + "INVALID";
		log("Using invalid aud in key binding JWT",
			args("original_aud", aud, "invalid_aud", invalidAud));
		return super.keyBindingJwt(privateKey, invalidAud, nonce, sdHash);
	}
}
