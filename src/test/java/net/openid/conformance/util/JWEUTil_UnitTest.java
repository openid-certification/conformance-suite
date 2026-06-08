package net.openid.conformance.util;

import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JWEUTil_UnitTest {

	String clientSecret = "secure";

	@Test
	public void supported_cbc_enc_algorithms_from_rfc7518_section_5_1_produce_the_expected_derived_key() {
		byte[] A128CBC_HS256_derivedKey = JWEUtil.deriveEncryptionKey("A128CBC-HS256", clientSecret);

		assertEquals("apNLRRRON1iRHvop7Wj7LUIPp71WhznNzaklH6lgmx4", Base64URL.encode(A128CBC_HS256_derivedKey).toString());
	}

	@Test
	public void supported_gcm_enc_algorithms_from_rfc7518_section_5_1_produce_the_expected_derived_key() {
		byte[] A128GCM_derivedKey = JWEUtil.deriveEncryptionKey("A128GCM", clientSecret);
		byte[] A192GCM_derivedKey = JWEUtil.deriveEncryptionKey("A192GCM", clientSecret);
		byte[] A256GCM_derivedKey = JWEUtil.deriveEncryptionKey("A256GCM", clientSecret);

		assertEquals("apNLRRRON1iRHvop7Wj7LQ", Base64URL.encode(A128GCM_derivedKey).toString());
		assertEquals("apNLRRRON1iRHvop7Wj7LUIPp71WhznN", Base64URL.encode(A192GCM_derivedKey).toString());
		assertEquals("apNLRRRON1iRHvop7Wj7LUIPp71WhznNzaklH6lgmx4", Base64URL.encode(A256GCM_derivedKey).toString());
	}

	@Test
	public void supported_key_wrap_algorithms_produce_the_expected_derived_key() {
		byte[] A128KW_derivedKey = JWEUtil.deriveEncryptionKey("A128KW", clientSecret);
		byte[] A128GCMKW_derivedKey = JWEUtil.deriveEncryptionKey("A128GCMKW", clientSecret);

		byte[] A192KW_derivedKey = JWEUtil.deriveEncryptionKey("A192KW", clientSecret);
		byte[] A192GCMKW_derivedKey = JWEUtil.deriveEncryptionKey("A192GCMKW", clientSecret);

		byte[] A256KW_derivedKey = JWEUtil.deriveEncryptionKey("A256KW", clientSecret);
		byte[] A256GCMKW_derivedKey = JWEUtil.deriveEncryptionKey("A256GCMKW", clientSecret);

		assertEquals("apNLRRRON1iRHvop7Wj7LQ", Base64URL.encode(A128KW_derivedKey).toString());
		assertEquals("apNLRRRON1iRHvop7Wj7LQ", Base64URL.encode(A128GCMKW_derivedKey).toString());

		assertEquals("apNLRRRON1iRHvop7Wj7LUIPp71WhznN", Base64URL.encode(A192KW_derivedKey).toString());
		assertEquals("apNLRRRON1iRHvop7Wj7LUIPp71WhznN", Base64URL.encode(A192GCMKW_derivedKey).toString());

		assertEquals("apNLRRRON1iRHvop7Wj7LUIPp71WhznNzaklH6lgmx4", Base64URL.encode(A256KW_derivedKey).toString());
		assertEquals("apNLRRRON1iRHvop7Wj7LUIPp71WhznNzaklH6lgmx4", Base64URL.encode(A256GCMKW_derivedKey).toString());
	}

	@Test
	public void unsupported_algorithms_from_rfc7518_section_5_1_includes_A192CBC_HS384() {
		Exception expectedException = assertThrows(RuntimeException.class,
			() -> JWEUtil.deriveEncryptionKey("A192CBC-HS384", clientSecret));

		assertEquals("Unexpected algorithm:A192CBC-HS384", expectedException.getMessage());
	}

	@Test
	public void unsupported_algorithms_from_rfc7518_section_5_1_includes_A256CBC_HS512() {
		Exception expectedException = assertThrows(RuntimeException.class,
			() -> JWEUtil.deriveEncryptionKey("A256CBC-HS512", clientSecret));

		assertEquals("Unexpected algorithm:A256CBC-HS512", expectedException.getMessage());
	}

	@Test
	public void throws_when_unable_to_parse_the_algorithm_for_the_key_length() {
		Exception expectedException = assertThrows(RuntimeException.class,
			() -> JWEUtil.deriveEncryptionKey("none", clientSecret));

		assertEquals("Unable to parse key bit length from algorithm none", expectedException.getMessage());
	}

	@Test
	public void selectAsymmetricKeyForEncryption_prefersMatchingKid() throws Exception {
		ECKey firstKey = new ECKeyGenerator(Curve.P_256)
			.algorithm(JWEAlgorithm.ECDH_ES)
			.keyUse(KeyUse.ENCRYPTION)
			.keyID(UUID.randomUUID().toString())
			.generate();
		ECKey secondKey = new ECKeyGenerator(Curve.P_256)
			.algorithm(JWEAlgorithm.ECDH_ES)
			.keyUse(KeyUse.ENCRYPTION)
			.keyID(UUID.randomUUID().toString())
			.generate();

		JWK jwk = JWEUtil.selectAsymmetricKeyForEncryption(
			new JWKSet(List.of(firstKey, secondKey)),
			JWEAlgorithm.ECDH_ES,
			secondKey.getKeyID());

		assertNotNull(jwk);
		assertEquals(secondKey.getKeyID(), jwk.getKeyID());
	}

	@Test
	public void selectAsymmetricKeyForEncryption_returnsNullWhenKidDoesNotMatch() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256)
			.algorithm(JWEAlgorithm.ECDH_ES)
			.keyUse(KeyUse.ENCRYPTION)
			.keyID(UUID.randomUUID().toString())
			.generate();

		JWK jwk = JWEUtil.selectAsymmetricKeyForEncryption(
			new JWKSet(List.of(key)),
			JWEAlgorithm.ECDH_ES,
			"missing-kid");

		assertNull(jwk);
	}

	@Test
	public void isValidJWEAlgorithm_acceptsRegisteredAsymmetricAndSymmetricAlgs() {
		assertTrue(JWEUtil.isValidJWEAlgorithm("RSA-OAEP"));
		assertTrue(JWEUtil.isValidJWEAlgorithm("RSA-OAEP-256"));
		assertTrue(JWEUtil.isValidJWEAlgorithm("ECDH-ES"));
		assertTrue(JWEUtil.isValidJWEAlgorithm("A128KW"));
		assertTrue(JWEUtil.isValidJWEAlgorithm("dir"));
	}

	@Test
	public void isValidJWEAlgorithm_rejectsUnknownNames() {
		assertFalse(JWEUtil.isValidJWEAlgorithm("RSA-OAEPxx"));
		assertFalse(JWEUtil.isValidJWEAlgorithm("ES256"));
		assertFalse(JWEUtil.isValidJWEAlgorithm(""));
	}

	@Test
	public void validAsymmetricJWEAlgorithms_excludesSymmetricAndDir() {
		var algs = JWEUtil.validAsymmetricJWEAlgorithms();
		assertTrue(algs.contains("RSA-OAEP-256"));
		assertTrue(algs.contains("ECDH-ES"));
		assertFalse(algs.contains("A128KW"));
		assertFalse(algs.contains("dir"));
	}

	@Test
	public void isValidEncryptionMethod_acceptsJWAEncMethods() {
		assertTrue(JWEUtil.isValidEncryptionMethod("A128CBC-HS256"));
		assertTrue(JWEUtil.isValidEncryptionMethod("A192CBC-HS384"));
		assertTrue(JWEUtil.isValidEncryptionMethod("A256CBC-HS512"));
		assertTrue(JWEUtil.isValidEncryptionMethod("A128GCM"));
		assertTrue(JWEUtil.isValidEncryptionMethod("A192GCM"));
		assertTrue(JWEUtil.isValidEncryptionMethod("A256GCM"));
	}

	@Test
	public void isValidEncryptionMethod_rejectsUnknownAndExtras() {
		assertFalse(JWEUtil.isValidEncryptionMethod("A128GCMxx"));
		// XC20P is a Nimbus extra but is not in the IANA JWA registry
		assertFalse(JWEUtil.isValidEncryptionMethod("XC20P"));
		assertFalse(JWEUtil.isValidEncryptionMethod(""));
	}

	@Test
	public void validEncryptionMethods_listsTheSixJwaMethods() {
		List<String> methods = JWEUtil.validEncryptionMethods();
		assertEquals(6, methods.size());
		assertTrue(methods.contains("A128CBC-HS256"));
		assertTrue(methods.contains("A192CBC-HS384"));
		assertTrue(methods.contains("A256CBC-HS512"));
		assertTrue(methods.contains("A128GCM"));
		assertTrue(methods.contains("A192GCM"));
		assertTrue(methods.contains("A256GCM"));
	}

}
