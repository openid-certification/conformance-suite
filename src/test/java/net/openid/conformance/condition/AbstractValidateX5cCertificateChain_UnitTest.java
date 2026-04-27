package net.openid.conformance.condition;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AbstractValidateX5cCertificateChain_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private TestableCondition cond;

	// Key pairs for test certificate generation
	private static KeyPair rootKeyPair;
	private static KeyPair intermediateKeyPair;
	private static KeyPair leafKeyPair;
	private static KeyPair unrelatedKeyPair;

	private static X509Certificate rootCert;       // self-signed root CA (trust anchor)
	private static X509Certificate intermediateCert; // signed by root
	private static X509Certificate leafCert;        // signed by intermediate
	private static X509Certificate leafFromRootCert; // signed directly by root (2-cert chain)
	private static X509Certificate selfSignedLeafCert; // self-signed leaf
	private static X509Certificate expiredLeafCert;  // expired, signed by root

	// PKIX-correct fixtures (with BasicConstraints + KeyUsage extensions on CAs)
	private static X509Certificate pkixRootCert;
	private static X509Certificate pkixIntermediateCert;
	private static X509Certificate pkixLeafCert;
	private static X509Certificate pkixLeafFromRootCert;        // 1-cert chain leaf signed by pkixRoot
	// Failing-variant intermediates (all signed by pkixRoot but each violating one PKIX rule)
	private static X509Certificate intermediateNoExtensions;
	private static X509Certificate intermediateBasicConstraintsCaFalse;
	private static X509Certificate intermediateNoKeyCertSign;
	private static X509Certificate intermediateExpired;
	private static X509Certificate leafForFailingIntermediates;

	@BeforeAll
	public static void generateCertificates() throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
		kpg.initialize(256);
		rootKeyPair = kpg.generateKeyPair();
		intermediateKeyPair = kpg.generateKeyPair();
		leafKeyPair = kpg.generateKeyPair();
		unrelatedKeyPair = kpg.generateKeyPair();

		Date notBefore = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
		Date notAfter = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));
		Date pastNotBefore = Date.from(Instant.now().minus(2, ChronoUnit.HOURS));
		Date pastNotAfter = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));

		// Self-signed root CA
		rootCert = generateCert("CN=Root CA", rootKeyPair, "CN=Root CA", rootKeyPair, notBefore, notAfter);

		// Intermediate signed by root
		intermediateCert = generateCert("CN=Intermediate CA", intermediateKeyPair,
			"CN=Root CA", rootKeyPair, notBefore, notAfter);

		// Leaf signed by intermediate
		leafCert = generateCert("CN=Leaf", leafKeyPair,
			"CN=Intermediate CA", intermediateKeyPair, notBefore, notAfter);

		// Leaf signed directly by root (for 2-cert chain tests)
		leafFromRootCert = generateCert("CN=Leaf Direct", leafKeyPair,
			"CN=Root CA", rootKeyPair, notBefore, notAfter);

		// Self-signed leaf
		selfSignedLeafCert = generateCert("CN=Self Signed Leaf", leafKeyPair,
			"CN=Self Signed Leaf", leafKeyPair, notBefore, notAfter);

		// Expired leaf signed by root
		expiredLeafCert = generateCert("CN=Expired Leaf", leafKeyPair,
			"CN=Root CA", rootKeyPair, pastNotBefore, pastNotAfter);

		// --- PKIX-correct fixtures ---
		// Root CA with BasicConstraints CA:true and KeyUsage keyCertSign|cRLSign
		pkixRootCert = generateCaCert("CN=PKIX Root CA", rootKeyPair, "CN=PKIX Root CA", rootKeyPair, notBefore, notAfter);
		// Intermediate CA signed by root, also with proper extensions
		pkixIntermediateCert = generateCaCert("CN=PKIX Intermediate CA", intermediateKeyPair, "CN=PKIX Root CA", rootKeyPair, notBefore, notAfter);
		// Leaf (end-entity) signed by intermediate, no CA extensions needed
		pkixLeafCert = generateCert("CN=PKIX Leaf", leafKeyPair, "CN=PKIX Intermediate CA", intermediateKeyPair, notBefore, notAfter);
		// Leaf signed directly by root for length-1 chain tests
		pkixLeafFromRootCert = generateCert("CN=PKIX Leaf Direct", leafKeyPair, "CN=PKIX Root CA", rootKeyPair, notBefore, notAfter);

		// Failing-variant intermediates: each signed by pkixRoot but violating one PKIX rule.
		// They issue leafForFailingIntermediates so a 2-cert chain can be built (leaf, intermediate).
		intermediateNoExtensions = generateCert("CN=Intermediate No Extensions", intermediateKeyPair, "CN=PKIX Root CA", rootKeyPair, notBefore, notAfter);
		intermediateBasicConstraintsCaFalse = generateCertWithExtensions("CN=Intermediate CA-False", intermediateKeyPair,
			"CN=PKIX Root CA", rootKeyPair, notBefore, notAfter,
			new BasicConstraints(false), new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
		intermediateNoKeyCertSign = generateCertWithExtensions("CN=Intermediate No KeyCertSign", intermediateKeyPair,
			"CN=PKIX Root CA", rootKeyPair, notBefore, notAfter,
			new BasicConstraints(true), new KeyUsage(KeyUsage.digitalSignature));
		intermediateExpired = generateCaCert("CN=Intermediate Expired", intermediateKeyPair,
			"CN=PKIX Root CA", rootKeyPair, pastNotBefore, pastNotAfter);
		// A leaf signed by the (failing) intermediate so we can build [leaf, intermediate] chains.
		leafForFailingIntermediates = generateCert("CN=Leaf for Failing Intermediates", leafKeyPair,
			"CN=Intermediate No Extensions", intermediateKeyPair, notBefore, notAfter);
	}

	private static X509Certificate generateCaCert(String subjectDN, KeyPair subjectKeyPair,
												  String issuerDN, KeyPair issuerKeyPair,
												  Date notBefore, Date notAfter) throws Exception {
		return generateCertWithExtensions(subjectDN, subjectKeyPair, issuerDN, issuerKeyPair, notBefore, notAfter,
			new BasicConstraints(true), new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
	}

	private static X509Certificate generateCertWithExtensions(String subjectDN, KeyPair subjectKeyPair,
															  String issuerDN, KeyPair issuerKeyPair,
															  Date notBefore, Date notAfter,
															  BasicConstraints basicConstraints,
															  KeyUsage keyUsage) throws Exception {
		X500Name subject = new X500Name(subjectDN);
		X500Name issuer = new X500Name(issuerDN);
		BigInteger serial = BigInteger.valueOf(System.nanoTime());

		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
			issuer, serial, notBefore, notAfter, subject, subjectKeyPair.getPublic());
		if (basicConstraints != null) {
			builder.addExtension(Extension.basicConstraints, true, basicConstraints);
		}
		if (keyUsage != null) {
			builder.addExtension(Extension.keyUsage, true, keyUsage);
		}

		ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
			.setProvider("BC")
			.build(issuerKeyPair.getPrivate());

		X509CertificateHolder holder = builder.build(signer);
		return new JcaX509CertificateConverter()
			.setProvider("BC")
			.getCertificate(holder);
	}

	private static X509Certificate generateCert(String subjectDN, KeyPair subjectKeyPair,
												 String issuerDN, KeyPair issuerKeyPair,
												 Date notBefore, Date notAfter) throws Exception {
		X500Name subject = new X500Name(subjectDN);
		X500Name issuer = new X500Name(issuerDN);
		BigInteger serial = BigInteger.valueOf(System.nanoTime());

		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
			issuer, serial, notBefore, notAfter, subject, subjectKeyPair.getPublic());

		ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
			.setProvider("BC")
			.build(issuerKeyPair.getPrivate());

		X509CertificateHolder holder = builder.build(signer);
		return new JcaX509CertificateConverter()
			.setProvider("BC")
			.getCertificate(holder);
	}

	@BeforeEach
	public void setUp() {
		cond = new TestableCondition();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	// -- Chain validation tests --

	@Test
	public void validTwoCertChainWithTrustAnchor() {
		assertDoesNotThrow(() ->
			cond.validateX5cCertificateChain(List.of(leafFromRootCert), rootCert));
	}

	@Test
	public void validThreeCertChainWithTrustAnchor() {
		assertDoesNotThrow(() ->
			cond.validateX5cCertificateChain(List.of(leafCert, intermediateCert), rootCert));
	}

	@Test
	public void validSingleCertNoTrustAnchor() {
		assertDoesNotThrow(() ->
			cond.validateX5cCertificateChain(List.of(leafFromRootCert), null));
	}

	@Test
	public void validTwoCertChainNoTrustAnchor() {
		assertDoesNotThrow(() ->
			cond.validateX5cCertificateChain(List.of(leafCert, intermediateCert), null));
	}

	@Test
	public void emptyChainFails() {
		assertThrows(ConditionError.class, () ->
			cond.validateX5cCertificateChain(List.of(), null));
	}

	@Test
	public void selfSignedLeafFails() {
		assertThrows(ConditionError.class, () ->
			cond.validateX5cCertificateChain(List.of(selfSignedLeafCert), null));
	}

	@Test
	public void expiredLeafFails() {
		assertThrows(ConditionError.class, () ->
			cond.validateX5cCertificateChain(List.of(expiredLeafCert), rootCert));
	}

	@Test
	public void trustAnchorInChainFails() {
		assertThrows(ConditionError.class, () ->
			cond.validateX5cCertificateChain(List.of(leafFromRootCert, rootCert), rootCert));
	}

	@Test
	public void lastCertSelfSignedWithoutTrustAnchorFails() {
		// Root cert is self-signed and last in chain — trust anchor exclusion
		assertThrows(ConditionError.class, () ->
			cond.validateX5cCertificateChain(List.of(leafFromRootCert, rootCert), null));
	}

	@Test
	public void brokenChainSignatureFails() {
		// leafFromRootCert is signed by root, not by intermediate — chain walk fails
		assertThrows(ConditionError.class, () ->
			cond.validateX5cCertificateChain(List.of(leafFromRootCert, intermediateCert), rootCert));
	}

	@Test
	public void lastCertNotSignedByTrustAnchorFails() {
		// intermediate is signed by root, but we pass unrelated cert as trust anchor
		X509Certificate unrelatedRoot;
		try {
			Date notBefore = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
			Date notAfter = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));
			unrelatedRoot = generateCert("CN=Unrelated Root", unrelatedKeyPair,
				"CN=Unrelated Root", unrelatedKeyPair, notBefore, notAfter);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		assertThrows(ConditionError.class, () ->
			cond.validateX5cCertificateChain(List.of(leafCert, intermediateCert), unrelatedRoot));
	}

	// -- Parsing tests --

	@Test
	public void parseFromStringsSucceeds() throws Exception {
		String encoded = Base64.getEncoder().encodeToString(leafFromRootCert.getEncoded());
		List<X509Certificate> parsed = cond.parseX5cCertificatesFromStrings(List.of(encoded));
		assertEquals(1, parsed.size());
		assertEquals(leafFromRootCert, parsed.get(0));
	}

	@Test
	public void parseFromStringsWithInvalidBase64Fails() {
		assertThrows(Exception.class, () ->
			cond.parseX5cCertificatesFromStrings(List.of("not-valid-base64!!!")));
	}

	@Test
	public void parseFromNimbusBase64Succeeds() throws Exception {
		com.nimbusds.jose.util.Base64 encoded =
			com.nimbusds.jose.util.Base64.encode(leafFromRootCert.getEncoded());
		List<X509Certificate> parsed = cond.parseX5cCertificatesFromNimbusBase64(List.of(encoded));
		assertEquals(1, parsed.size());
		assertEquals(leafFromRootCert, parsed.get(0));
	}

	// -- JWT signature verification tests --

	@Test
	public void verifyJwtSignatureWithValidCert() throws Exception {
		String jwt = createSignedJwt(leafKeyPair);
		assertDoesNotThrow(() ->
			cond.verifyJwtSignatureWithX5cLeafCert(jwt, List.of(leafFromRootCert)));
	}

	@Test
	public void verifyJwtSignatureWithWrongCertFails() throws Exception {
		// JWT signed by leaf key, but we pass a cert with a different public key
		String jwt = createSignedJwt(leafKeyPair);
		X509Certificate wrongCert;
		Date notBefore = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
		Date notAfter = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));
		wrongCert = generateCert("CN=Wrong", unrelatedKeyPair,
			"CN=Root CA", rootKeyPair, notBefore, notAfter);
		assertThrows(ConditionError.class, () ->
			cond.verifyJwtSignatureWithX5cLeafCert(jwt, List.of(wrongCert)));
	}

	private static String createSignedJwt(KeyPair keyPair) throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.subject("test")
			.issueTime(new Date())
			.build();
		SignedJWT jwt = new SignedJWT(header, claims);
		jwt.sign(new ECDSASigner((ECPrivateKey) keyPair.getPrivate()));
		return jwt.serialize();
	}

	// -- Strict PKIX mode tests --

	@Test
	public void strictMode_validThreeCertChain_passes() {
		assertDoesNotThrow(() ->
			cond.validateX5cCertificateChain(List.of(pkixLeafCert, pkixIntermediateCert), pkixRootCert, true));
	}

	@Test
	public void strictMode_validSingleLeafChain_passesWhenLeafSignedByAnchor() {
		assertDoesNotThrow(() ->
			cond.validateX5cCertificateChain(List.of(pkixLeafFromRootCert), pkixRootCert, true));
	}

	@Test
	public void strictMode_nullTrustAnchor_fallsBackToLegacyWalk() {
		// Strict PKIX requires a trust anchor; when none is configured the helper falls
		// through to the legacy walk rather than blocking the test outright. The fail-fast
		// "trust anchor required" UX lives in the EnsureCredentialTrustAnchorConfigured /
		// EnsureClientRequestObjectTrustAnchorConfigured preconditions wired into test modules.
		// The 2-cert chain is leaf+intermediate-from-root; the legacy walk verifies the
		// intermediate is not self-signed (trust anchor exclusion) and walks parent signatures.
		assertDoesNotThrow(() ->
			cond.validateX5cCertificateChain(List.of(pkixLeafCert, pkixIntermediateCert), null, true));

		verify(eventLog).log(eq(TestableCondition.class.getSimpleName()), argThat((String msg) ->
			msg != null && msg.contains("falling back to legacy chain walk")));
	}

	@Test
	public void strictMode_expiredIntermediate_fails() {
		// Proves the gap exists today — non-strict mode currently passes this case.
		// pkixLeaf is current-time-valid, but intermediateExpired is in the past.
		// The leaf-of-chain validity check passes (leaf is valid); PKIX catches the expired intermediate.
		X509Certificate leafSignedByExpiredIntermediate;
		try {
			Date notBefore = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
			Date notAfter = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));
			leafSignedByExpiredIntermediate = generateCert("CN=Leaf Sig By Expired Intermediate", leafKeyPair,
				"CN=Intermediate Expired", intermediateKeyPair, notBefore, notAfter);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		assertThrows(ConditionError.class, () ->
			cond.validateX5cCertificateChain(List.of(leafSignedByExpiredIntermediate, intermediateExpired), pkixRootCert, true));
	}

	@Test
	public void strictMode_intermediateMissingBasicConstraints_fails() {
		assertThrows(ConditionError.class, () ->
			cond.validateX5cCertificateChain(List.of(leafForFailingIntermediates, intermediateNoExtensions), pkixRootCert, true));
	}

	@Test
	public void strictMode_intermediateBasicConstraintsCaFalse_fails() {
		assertThrows(ConditionError.class, () ->
			cond.validateX5cCertificateChain(List.of(leafForFailingIntermediates, intermediateBasicConstraintsCaFalse), pkixRootCert, true));
	}

	@Test
	public void strictMode_intermediateMissingKeyCertSign_fails() {
		assertThrows(ConditionError.class, () ->
			cond.validateX5cCertificateChain(List.of(leafForFailingIntermediates, intermediateNoKeyCertSign), pkixRootCert, true));
	}

	@Test
	public void nonStrictMode_existingFixturesStillPass() {
		// Pin the regression: the legacy walk does NOT enforce PKIX checks even in strict-failing scenarios.
		assertDoesNotThrow(() ->
			cond.validateX5cCertificateChain(List.of(leafForFailingIntermediates, intermediateNoExtensions), pkixRootCert, false));
	}

	/**
	 * Concrete subclass to make protected methods accessible in tests.
	 */
	static class TestableCondition extends AbstractValidateX5cCertificateChain {
		@Override
		public Environment evaluate(Environment env) {
			return env;
		}
	}
}
