package net.openid.conformance.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyManager_UnitTest {

	private static KeyManager keyManager(String privateLinkSigningKey) {
		KeyManager keyManager = new KeyManager();
		ReflectionTestUtils.setField(keyManager, "signingKey", "");
		ReflectionTestUtils.setField(keyManager, "deprecatedSigningKey", "");
		ReflectionTestUtils.setField(keyManager, "privateLinkSigningKey", privateLinkSigningKey);
		return keyManager;
	}

	private static String base64(String value) {
		return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private static RSAKey generateRsaKey(String kid) throws JOSEException {
		return new RSAKeyGenerator(2048)
			.keyID(kid)
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.RS256)
			.generate();
	}

	@Test
	void undecodable_base64_private_link_key_fails_with_clear_message() {
		KeyManager keyManager = keyManager("not base64!");

		IllegalStateException e = assertThrows(IllegalStateException.class, keyManager::initializeKeyManager);
		assertEquals("Couldn't decode private link signing key", e.getMessage());
	}

	@Test
	void private_link_key_that_is_not_a_jwk_fails_with_clear_message() {
		KeyManager keyManager = keyManager(base64("{\"not\":\"a jwk\"}"));

		IllegalStateException e = assertThrows(IllegalStateException.class, keyManager::initializeKeyManager);
		assertEquals("Couldn't decode private link signing key", e.getMessage());
	}

	@Test
	void configured_private_link_key_is_used() throws JOSEException {
		RSAKey configured = generateRsaKey("private-link-test");
		KeyManager keyManager = keyManager(base64(configured.toJSONString()));

		keyManager.initializeKeyManager();

		assertTrue(keyManager.privateLinkKeyWasConfigured());
		assertEquals(configured, keyManager.getPrivateLinkKey());
	}

	@Test
	void missing_private_link_key_is_generated() {
		KeyManager keyManager = keyManager("");

		keyManager.initializeKeyManager();

		assertFalse(keyManager.privateLinkKeyWasConfigured());
		assertNotNull(keyManager.getPrivateLinkKey());
		assertTrue(keyManager.getPrivateLinkKey().isPrivate());
	}
}
