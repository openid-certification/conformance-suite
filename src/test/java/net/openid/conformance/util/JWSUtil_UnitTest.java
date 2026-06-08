package net.openid.conformance.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JWSUtil_UnitTest {

	@Test
	void recognisesCommonAlgs() {
		assertTrue(JWSUtil.isValidJWSAlgorithm("ES256"));
		assertTrue(JWSUtil.isValidJWSAlgorithm("RS256"));
		assertTrue(JWSUtil.isValidJWSAlgorithm("PS256"));
		assertTrue(JWSUtil.isValidJWSAlgorithm("HS256"));
		assertTrue(JWSUtil.isValidJWSAlgorithm("EdDSA"));
	}

	@Test
	void recognisesPostQuantumAlgs() {
		assertTrue(JWSUtil.isValidJWSAlgorithm("ML-DSA-44"));
		assertTrue(JWSUtil.isValidJWSAlgorithm("ML-DSA-65"));
		assertTrue(JWSUtil.isValidJWSAlgorithm("ML-DSA-87"));
	}

	@Test
	void recognisesEd25519AndEd448() {
		assertTrue(JWSUtil.isValidJWSAlgorithm("Ed25519"));
		assertTrue(JWSUtil.isValidJWSAlgorithm("Ed448"));
	}

	@Test
	void rejectsTypoAlgs() {
		assertFalse(JWSUtil.isValidJWSAlgorithm("ES265"));
		assertFalse(JWSUtil.isValidJWSAlgorithm("ES2665K"));
		assertFalse(JWSUtil.isValidJWSAlgorithm("ML-DSA-128"));
	}

	@Test
	void isAsymmetricExcludesMAC() {
		assertFalse(JWSUtil.isAsymmetricJWSAlgorithm("HS256"));
		assertFalse(JWSUtil.isAsymmetricJWSAlgorithm("HS384"));
		assertFalse(JWSUtil.isAsymmetricJWSAlgorithm("HS512"));
	}

	@Test
	void isAsymmetricIncludesEcRsaEdAndMlDsa() {
		assertTrue(JWSUtil.isAsymmetricJWSAlgorithm("ES256"));
		assertTrue(JWSUtil.isAsymmetricJWSAlgorithm("RS256"));
		assertTrue(JWSUtil.isAsymmetricJWSAlgorithm("PS256"));
		assertTrue(JWSUtil.isAsymmetricJWSAlgorithm("EdDSA"));
		assertTrue(JWSUtil.isAsymmetricJWSAlgorithm("ML-DSA-65"));
	}

	@Test
	void validAlgorithmsListIncludesAllFamiliesAndExtras() {
		var algs = JWSUtil.validJWSAlgorithms();
		assertTrue(algs.contains("ES256"));
		assertTrue(algs.contains("HS256"));
		assertTrue(algs.contains("RS256"));
		assertTrue(algs.contains("EdDSA"));
		assertTrue(algs.contains("ML-DSA-44"));
	}

	@Test
	void validAsymmetricListExcludesMACAndIncludesExtras() {
		var algs = JWSUtil.validAsymmetricJWSAlgorithms();
		assertTrue(algs.contains("ES256"));
		assertTrue(algs.contains("RS256"));
		assertTrue(algs.contains("EdDSA"));
		assertTrue(algs.contains("ML-DSA-87"));
		assertFalse(algs.contains("HS256"));
		assertFalse(algs.contains("HS384"));
		assertFalse(algs.contains("HS512"));
	}
}
