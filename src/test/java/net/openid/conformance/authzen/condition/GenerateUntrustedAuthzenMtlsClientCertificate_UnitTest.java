package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MtlsKeyUtil;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateUntrustedAuthzenMtlsClientCertificate_UnitTest {

	private static final X500Principal SUBJECT = new X500Principal("CN=authzen-conformance-pep");

	private static final X500Principal ISSUER = new X500Principal("CN=Example PEP CA");

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private GenerateUntrustedAuthzenMtlsClientCertificate cond;

	@BeforeEach
	public void setUp() {
		cond = new GenerateUntrustedAuthzenMtlsClientCertificate();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	/** A certificate for {@code subjectKeys} issued by a separate CA key, as a configured client certificate would be. */
	private static X509Certificate issue(KeyPair subjectKeys, KeyPair caKeys, String signatureAlgorithm) throws Exception {
		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(ISSUER, BigInteger.valueOf(4242),
			new Date(System.currentTimeMillis() - 86_400_000L), new Date(System.currentTimeMillis() + 86_400_000L),
			SUBJECT, subjectKeys.getPublic());
		return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder(signatureAlgorithm).build(caKeys.getPrivate())));
	}

	private void configure(X509Certificate certificate) throws Exception {
		JsonObject mtls = new JsonObject();
		mtls.addProperty("cert", Base64.getEncoder().encodeToString(certificate.getEncoded()));
		mtls.addProperty("key", "configured-key-not-used");
		env.putObject("mutual_tls_authentication", mtls);
	}

	private X509Certificate untrustedCertificate() throws Exception {
		String cert = env.getString("untrusted_mutual_tls_authentication", "cert");
		return (X509Certificate) CertificateFactory.getInstance("X.509")
			.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(cert)));
	}

	private PrivateKey untrustedKey(String algorithm) throws Exception {
		String key = env.getString("untrusted_mutual_tls_authentication", "key");
		return KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key)));
	}

	private static void assertKeyMatchesCertificate(PrivateKey key, X509Certificate certificate, String signatureAlgorithm) throws Exception {
		byte[] data = "proof of possession".getBytes(StandardCharsets.UTF_8);
		Signature signer = Signature.getInstance(signatureAlgorithm);
		signer.initSign(key);
		signer.update(data);
		byte[] signature = signer.sign();
		Signature verifier = Signature.getInstance(signatureAlgorithm);
		verifier.initVerify(certificate.getPublicKey());
		verifier.update(data);
		assertTrue(verifier.verify(signature));
	}

	@Test
	public void rsaCertificate_isReplacedBySelfSignedCertificateWithSameNames() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair subjectKeys = generator.generateKeyPair();
		X509Certificate configured = issue(subjectKeys, generator.generateKeyPair(), "SHA256withRSA");
		configure(configured);

		cond.execute(env);

		X509Certificate untrusted = untrustedCertificate();
		assertEquals(configured.getSubjectX500Principal(), untrusted.getSubjectX500Principal());
		assertEquals(configured.getIssuerX500Principal(), untrusted.getIssuerX500Principal());
		assertEquals(configured.getSerialNumber(), untrusted.getSerialNumber());
		assertEquals("RSA", untrusted.getPublicKey().getAlgorithm());
		assertNotEquals(configured.getPublicKey(), untrusted.getPublicKey());
		// Signed by its own key, not by the issuer the configured certificate names.
		untrusted.verify(untrusted.getPublicKey());
		assertKeyMatchesCertificate(untrustedKey("RSA"), untrusted, "SHA256withRSA");
	}

	@Test
	public void ecCertificate_getsAKeyOnTheSameCurve() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(new ECGenParameterSpec("secp384r1"));
		X509Certificate configured = issue(generator.generateKeyPair(), generator.generateKeyPair(), "SHA384withECDSA");
		configure(configured);

		cond.execute(env);

		X509Certificate untrusted = untrustedCertificate();
		assertEquals(configured.getSubjectX500Principal(), untrusted.getSubjectX500Principal());
		assertEquals("EC", untrusted.getPublicKey().getAlgorithm());
		assertEquals(((java.security.interfaces.ECPublicKey) configured.getPublicKey()).getParams().getCurve(),
			((java.security.interfaces.ECPublicKey) untrusted.getPublicKey()).getParams().getCurve());
		untrusted.verify(untrusted.getPublicKey());
		assertKeyMatchesCertificate(untrustedKey("EC"), untrusted, "SHA256withECDSA");
	}

	@Test
	public void ed25519Certificate_getsAnEd25519Key() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
		X509Certificate configured = issue(generator.generateKeyPair(), generator.generateKeyPair(), "Ed25519");
		configure(configured);

		cond.execute(env);

		X509Certificate untrusted = untrustedCertificate();
		assertEquals(configured.getSubjectX500Principal(), untrusted.getSubjectX500Principal());
		assertEquals(configured.getIssuerX500Principal(), untrusted.getIssuerX500Principal());
		assertEquals("Ed25519", ((EdECPublicKey) untrusted.getPublicKey()).getParams().getName());
		assertNotEquals(configured.getPublicKey(), untrusted.getPublicKey());
		untrusted.verify(untrusted.getPublicKey());
		assertKeyMatchesCertificate(untrustedKey("Ed25519"), untrusted, "Ed25519");
		// The suite's mTLS client loads the key by the certificate's key algorithm, as it does a configured one.
		MtlsKeyUtil.generateAlgPrivateKeyFromDER(untrusted.getPublicKey().getAlgorithm(),
			Base64.getDecoder().decode(env.getString("untrusted_mutual_tls_authentication", "key")));
	}

	@Test
	public void ed448Certificate_getsAnEd448Key() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed448");
		X509Certificate configured = issue(generator.generateKeyPair(), generator.generateKeyPair(), "Ed448");
		configure(configured);

		cond.execute(env);

		X509Certificate untrusted = untrustedCertificate();
		assertEquals("Ed448", ((EdECPublicKey) untrusted.getPublicKey()).getParams().getName());
		untrusted.verify(untrusted.getPublicKey());
		assertKeyMatchesCertificate(untrustedKey("Ed448"), untrusted, "Ed448");
	}

	@Test
	public void dsaCertificate_fails() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("DSA");
		generator.initialize(2048);
		configure(issue(generator.generateKeyPair(), generator.generateKeyPair(), "SHA256withDSA"));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not RSA, EC or EdDSA"), e.getMessage());
	}

	@Test
	public void certificateNotBase64_fails() {
		JsonObject mtls = new JsonObject();
		mtls.addProperty("cert", "not base64!");
		env.putObject("mutual_tls_authentication", mtls);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingCertificate_fails() {
		env.putObject("mutual_tls_authentication", new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
