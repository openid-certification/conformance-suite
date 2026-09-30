package net.openid.conformance.settings;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PemParsing_UnitTest {

	private static KeyPair rsaKeys;
	private static KeyPair otherRsaKeys;
	private static KeyPair ecKeys;
	private static X509Certificate rsaCertificate;
	private static X509Certificate otherRsaCertificate;
	private static X509Certificate ecCertificate;

	@BeforeAll
	public static void createKeys() throws Exception {
		rsaKeys = TestPki.rsaKeyPair();
		otherRsaKeys = TestPki.rsaKeyPair();
		ecKeys = TestPki.ecKeyPair();
		rsaCertificate = TestPki.certificate(rsaKeys, "leaf", otherRsaKeys, "issuer",
			java.time.Instant.now().minusSeconds(60), java.time.Instant.now().plusSeconds(3600));
		otherRsaCertificate = TestPki.selfSigned(otherRsaKeys, "issuer");
		ecCertificate = TestPki.selfSigned(ecKeys, "ec");
	}

	@Test
	public void parsesAChainInOrder() throws Exception {
		List<X509Certificate> chain = PemParsing.parseCertificateChain(TestPki.pem(rsaCertificate, otherRsaCertificate));

		assertThat(chain).containsExactly(rsaCertificate, otherRsaCertificate);
	}

	@Test
	public void parsesAChainWithCrlfLineEndingsAndSurroundingWhitespace() throws Exception {
		String pem = "\n  " + TestPki.pem(rsaCertificate).replace("\n", "\r\n") + "  \n";

		assertThat(PemParsing.parseCertificateChain(pem)).containsExactly(rsaCertificate);
	}

	@Test
	public void rejectsABlankChain() {
		assertThatThrownBy(() -> PemParsing.parseCertificateChain("  "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("no certificate found");
	}

	@Test
	public void rejectsANullChain() {
		assertThatThrownBy(() -> PemParsing.parseCertificateChain(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("no certificate found");
	}

	@Test
	public void rejectsTextThatIsNotPem() {
		assertThatThrownBy(() -> PemParsing.parseCertificateChain("not a certificate"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("no certificate found");
	}

	@Test
	public void rejectsACertificateBlockWithInvalidBase64() {
		String pem = "-----BEGIN CERTIFICATE-----\nnot*base64!!\n-----END CERTIFICATE-----\n";

		assertThatThrownBy(() -> PemParsing.parseCertificateChain(pem))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("not a valid PEM certificate");
	}

	@Test
	public void rejectsAKeyBlockWithInvalidBase64() {
		String pem = "-----BEGIN PRIVATE KEY-----\n@@@@\n-----END PRIVATE KEY-----\n";

		assertThatThrownBy(() -> PemParsing.parsePrivateKey(pem))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("not a valid PEM private key");
	}

	@Test
	public void rejectsAPrivateKeyInTheChain() throws Exception {
		String pem = TestPki.pem(rsaCertificate) + TestPki.pkcs8Pem(rsaKeys.getPrivate());

		assertThatThrownBy(() -> PemParsing.parseCertificateChain(pem))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("something other than a certificate");
	}

	@Test
	public void parsesAPkcs1RsaKey() throws Exception {
		String pem = TestPki.pem(rsaKeys.getPrivate());
		assertThat(pem).contains("BEGIN RSA PRIVATE KEY");

		assertThat(PemParsing.parsePrivateKey(pem).getEncoded()).isEqualTo(rsaKeys.getPrivate().getEncoded());
	}

	@Test
	public void parsesAPkcs8RsaKey() throws Exception {
		String pem = TestPki.pkcs8Pem(rsaKeys.getPrivate());
		assertThat(pem).contains("BEGIN PRIVATE KEY");

		assertThat(PemParsing.parsePrivateKey(pem).getEncoded()).isEqualTo(rsaKeys.getPrivate().getEncoded());
	}

	@Test
	public void parsesASec1EcKey() throws Exception {
		String pem = TestPki.sec1Pem(ecKeys.getPrivate());
		assertThat(pem).contains("BEGIN EC PRIVATE KEY");

		PrivateKey key = PemParsing.parsePrivateKey(pem);

		assertThat(key.getAlgorithm()).isIn("EC", "ECDSA");
		assertThat(PemParsing.keyMatchesCertificate(key, ecCertificate)).isTrue();
	}

	@Test
	public void parsesAPkcs8EcKey() throws Exception {
		PrivateKey key = PemParsing.parsePrivateKey(TestPki.pkcs8Pem(ecKeys.getPrivate()));

		assertThat(key.getAlgorithm()).isIn("EC", "ECDSA");
		assertThat(PemParsing.keyMatchesCertificate(key, ecCertificate)).isTrue();
	}

	@Test
	public void rejectsAnEncryptedKey() throws Exception {
		String pem = TestPki.encryptedPkcs8Pem(rsaKeys.getPrivate());

		assertThatThrownBy(() -> PemParsing.parsePrivateKey(pem))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("encrypted private keys are not supported");
	}

	@Test
	public void rejectsTwoKeysInOnePem() throws Exception {
		String pem = TestPki.pkcs8Pem(rsaKeys.getPrivate()) + TestPki.pkcs8Pem(otherRsaKeys.getPrivate());

		assertThatThrownBy(() -> PemParsing.parsePrivateKey(pem))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("more than one");
	}

	@Test
	public void rejectsACertificateAsAKey() throws Exception {
		assertThatThrownBy(() -> PemParsing.parsePrivateKey(TestPki.pem(rsaCertificate)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("not a private key");
	}

	@Test
	public void rejectsABlankKey() {
		assertThatThrownBy(() -> PemParsing.parsePrivateKey(""))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("no private key found");
	}

	@Test
	public void theCertificatesOwnKeyMatches() {
		assertThat(PemParsing.keyMatchesCertificate(rsaKeys.getPrivate(), rsaCertificate)).isTrue();
	}

	@Test
	public void anotherRsaKeyDoesNotMatch() {
		assertThat(PemParsing.keyMatchesCertificate(otherRsaKeys.getPrivate(), rsaCertificate)).isFalse();
	}

	@Test
	public void anEcKeyDoesNotMatchAnRsaCertificate() {
		assertThat(PemParsing.keyMatchesCertificate(ecKeys.getPrivate(), rsaCertificate)).isFalse();
	}
}
