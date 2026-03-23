package net.openid.conformance.condition.as;

import com.nimbusds.jose.jwk.ECKey;

public class CreateSdJwtKbCredentialWithInvalidSdHash extends CreateSdJwtKbCredential {

	@Override
	public String keyBindingJwt(ECKey privateKey, String aud, String nonce, String sdHash) {
		String invalidSdHash = sdHash + "INVALID";
		log("Using invalid sd_hash in key binding JWT",
			args("original_sd_hash", sdHash, "invalid_sd_hash", invalidSdHash));
		return super.keyBindingJwt(privateKey, aud, nonce, invalidSdHash);
	}
}
