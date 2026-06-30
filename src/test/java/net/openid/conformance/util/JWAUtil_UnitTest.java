package net.openid.conformance.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JWAUtil_UnitTest {

	@Test
	public void ed25519UsesSha512LikeEdDSA() throws JWAUtil.InvalidAlgorithmException {
		assertEquals("SHA-512", JWAUtil.getDigestAlgorithmForSigAlg("Ed25519"));
		assertEquals("SHA-512", JWAUtil.getDigestAlgorithmForSigAlg("EdDSA"));
	}

	@Test
	public void es256UsesSha256() throws JWAUtil.InvalidAlgorithmException {
		assertEquals("SHA-256", JWAUtil.getDigestAlgorithmForSigAlg("ES256"));
	}

	@Test
	public void invalidAlgorithmThrows() {
		assertThrows(JWAUtil.InvalidAlgorithmException.class,
			() -> JWAUtil.getDigestAlgorithmForSigAlg("not-an-alg"));
	}
}
