package net.openid.conformance.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	public void signatureAndMacAlgorithmsAreJwsAlgorithms() {
		for (String alg : new String[]{"HS256", "HS384", "HS512", "RS256", "RS512", "PS256", "PS512",
			"ES256", "ES256K", "ES384", "ES512", "EdDSA", "Ed25519", "Ed448"}) {
			assertTrue(JWAUtil.isJwsAlgorithm(alg), alg + " is a registered JWS algorithm");
		}
	}

	@Test
	public void jweAlgorithmsAreNotJwsAlgorithms() {
		// These are the ones a "does this algorithm have a key type?" test would wrongly accept:
		// KeyType.forAlgorithm() maps every one of them onto a key type.
		for (String alg : new String[]{"dir", "A128KW", "A256GCMKW", "RSA-OAEP-256", "ECDH-ES",
			"ECDH-ES+A128KW", "PBES2-HS256+A128KW"}) {
			assertFalse(JWAUtil.isJwsAlgorithm(alg), alg + " is a JWE algorithm, not a JWS one");
		}
	}

	@Test
	public void noneAndUnknownAndEmptyAreNotJwsAlgorithms() {
		assertFalse(JWAUtil.isJwsAlgorithm("none"));
		assertFalse(JWAUtil.isJwsAlgorithm("FOO256"));
		assertFalse(JWAUtil.isJwsAlgorithm("RS256 "));
		assertFalse(JWAUtil.isJwsAlgorithm("rs256"));
		assertFalse(JWAUtil.isJwsAlgorithm(""));
		assertFalse(JWAUtil.isJwsAlgorithm(null));
	}
}
