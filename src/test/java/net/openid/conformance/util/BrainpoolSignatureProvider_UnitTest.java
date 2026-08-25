package net.openid.conformance.util;

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BrainpoolSignatureProvider_UnitTest {

	private static final byte[] DATA = "brainpool provider test data".getBytes(StandardCharsets.UTF_8);

	@BeforeAll
	public static void installProvider() {
		BrainpoolSignatureProvider.ensureInstalled();
	}

	private KeyPair generateKeyPair(String curveName) throws Exception {
		// generate with BC explicitly: key generation is not what this provider mediates
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", BouncyCastleProviderSingleton.getInstance());
		generator.initialize(new ECGenParameterSpec(curveName));
		return generator.generateKeyPair();
	}

	/** Sign+verify round trip through the provider-less JCA lookup that multipaz uses. */
	private void roundTrip(String curveName, String algorithm, String expectedProvider) throws Exception {
		KeyPair keyPair = generateKeyPair(curveName);

		Signature signer = Signature.getInstance(algorithm);
		signer.initSign(keyPair.getPrivate());
		assertEquals(expectedProvider, signer.getProvider().getName(), "signing provider for " + curveName);
		signer.update(DATA);
		byte[] signature = signer.sign();

		Signature verifier = Signature.getInstance(algorithm);
		verifier.initVerify(keyPair.getPublic());
		assertEquals(expectedProvider, verifier.getProvider().getName(), "verifying provider for " + curveName);
		verifier.update(DATA);
		assertTrue(verifier.verify(signature), "signature over " + curveName + " must verify");

		Signature rejecting = Signature.getInstance(algorithm);
		rejecting.initVerify(keyPair.getPublic());
		rejecting.update("tampered".getBytes(StandardCharsets.UTF_8));
		assertFalse(rejecting.verify(signature), "signature over tampered data must not verify");
	}

	@Test
	public void testBrainpoolKeysAreHandledByThisProvider() throws Exception {
		roundTrip("brainpoolP256r1", "SHA256withECDSA", BrainpoolSignatureProvider.NAME);
	}

	@Test
	public void testBrainpoolP512AlsoWorks() throws Exception {
		roundTrip("brainpoolP512r1", "SHA512withECDSA", BrainpoolSignatureProvider.NAME);
	}

	/**
	 * The regression guard for JCA delayed provider selection: with this provider installed at
	 * position 1, non-brainpool keys must still fall through to the standard provider (SunEC),
	 * keeping behaviour on every existing path (TLS, JWS, PKIX) unchanged.
	 */
	@Test
	public void testNistCurvesFallThroughToSunEC() throws Exception {
		roundTrip("secp256r1", "SHA256withECDSA", "SunEC");
	}

	@Test
	public void testIsBrainpoolKeyDetection() throws Exception {
		assertTrue(BrainpoolSignatureProvider.isBrainpoolKey(generateKeyPair("brainpoolP256r1").getPublic()));
		assertTrue(BrainpoolSignatureProvider.isBrainpoolKey(generateKeyPair("brainpoolP384r1").getPrivate()));
		assertFalse(BrainpoolSignatureProvider.isBrainpoolKey(generateKeyPair("secp256r1").getPublic()));
		assertFalse(BrainpoolSignatureProvider.isBrainpoolKey(generateKeyPair("secp521r1").getPrivate()));
	}

	@Test
	public void testProviderInstalledAtHighestPriority() {
		assertEquals(BrainpoolSignatureProvider.NAME, Security.getProviders()[0].getName());
	}

	/**
	 * Documents a known limitation, not a promise: getProvider() (like getParameters() or
	 * setParameter()) before init fixes JCA provider selection to this position-1 provider, so
	 * a later non-brainpool init throws instead of falling through to SunEC - the SPI cannot
	 * tell early fixation apart from selection-time probing. No known ECDSA caller uses this
	 * pre-init pattern (it is an RSASSA-PSS idiom, and PSS is not registered here); a caller
	 * that does must request a provider explicitly, as the exception message says. If this
	 * test ever fails because such a caller appeared and the design changed, update the class
	 * javadoc and the commit-message caveat too.
	 */
	@Test
	public void testProviderFixedBeforeInitCannotFallThrough() throws Exception {
		Signature fixedEarly = Signature.getInstance("SHA256withECDSA");
		assertEquals(BrainpoolSignatureProvider.NAME, fixedEarly.getProvider().getName());
		KeyPair nistKeyPair = generateKeyPair("secp256r1");
		assertThrows(InvalidKeyException.class, () -> fixedEarly.initVerify(nistKeyPair.getPublic()));

		// brainpool keys are unaffected: this provider is the right selection for them anyway
		Signature brainpool = Signature.getInstance("SHA256withECDSA");
		brainpool.getProvider();
		brainpool.initVerify(generateKeyPair("brainpoolP256r1").getPublic());
	}
}
