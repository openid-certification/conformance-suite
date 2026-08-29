package net.openid.conformance.util;

import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the runtime-minted mdoc document signer certificate against the DS certificate
 * profile in ISO/IEC 18013-5 Table B.3 (see issue #1891 — the previous hard-coded DS
 * certificate was self-signed with CA extensions).
 */
public class TestKeysAndCerts_UnitTest {

	private static X509Certificate dsCert;
	private static X509Certificate iacaCert;
	private static X509Certificate statusListSignerCert;

	@BeforeAll
	public static void setUp() throws Exception {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		dsCert = (X509Certificate) cf.generateCertificate(
			new ByteArrayInputStream(bytes(TestKeysAndCerts.getDocumentSignerCert().getEncoded())));
		iacaCert = (X509Certificate) cf.generateCertificate(
			new ByteArrayInputStream(bytes(TestKeysAndCerts.getIacaRootCert().getEncoded())));
		statusListSignerCert = (X509Certificate) cf.generateCertificate(
			new ByteArrayInputStream(bytes(TestKeysAndCerts.getStatusListSignerCert().getEncoded())));
	}

	@Test
	public void isVersion3() {
		assertThat(dsCert.getVersion()).isEqualTo(3);
	}

	@Test
	public void serialNumberIsPositiveAndAtMost20Octets() {
		assertThat(dsCert.getSerialNumber().signum()).isEqualTo(1);
		assertThat(dsCert.getSerialNumber().toByteArray().length).isLessThanOrEqualTo(20);
	}

	@Test
	public void isIssuedAndSignedByIacaRoot() throws Exception {
		assertThat(dsCert.getIssuerX500Principal()).isEqualTo(iacaCert.getSubjectX500Principal());
		dsCert.verify(iacaCert.getPublicKey());
	}

	@Test
	public void signatureAlgorithmIsEcdsaWithSha256() {
		assertThat(dsCert.getSigAlgOID()).isEqualTo("1.2.840.10045.4.3.2");
	}

	@Test
	public void isCurrentlyValidAndValidityDoesNotExceed457Days() {
		Date now = new Date();
		assertThat(dsCert.getNotBefore()).isBefore(now);
		assertThat(dsCert.getNotAfter()).isAfter(now);
		long validityMillis = dsCert.getNotAfter().getTime() - dsCert.getNotBefore().getTime();
		assertThat(validityMillis).isLessThanOrEqualTo(Duration.ofDays(457).toMillis());
	}

	@Test
	public void keyUsageIsCriticalAndDigitalSignatureOnly() {
		assertThat(dsCert.getCriticalExtensionOIDs()).contains("2.5.29.15");
		boolean[] keyUsage = dsCert.getKeyUsage();
		assertThat(keyUsage).isNotNull();
		// RFC 5280 bit order: digitalSignature is bit 0; all other bits must be 0 per Table B.3
		assertThat(keyUsage[0]).isTrue();
		for (int i = 1; i < keyUsage.length; i++) {
			assertThat(keyUsage[i]).as("keyUsage bit %d must not be set", i).isFalse();
		}
	}

	@Test
	public void hasNoBasicConstraints() {
		assertThat(dsCert.getBasicConstraints()).isEqualTo(-1);
		assertThat(dsCert.getExtensionValue("2.5.29.19")).isNull();
	}

	@Test
	public void extendedKeyUsageIsCriticalMdlDsOnly() throws Exception {
		assertThat(dsCert.getCriticalExtensionOIDs()).contains("2.5.29.37");
		assertThat(dsCert.getExtendedKeyUsage()).containsExactly("1.0.18013.5.1.2");
	}

	@Test
	public void onlyKeyUsageAndExtendedKeyUsageAreCritical() {
		// Table B.3: further extensions may be present only if marked non-critical
		assertThat(dsCert.getCriticalExtensionOIDs()).containsExactlyInAnyOrder("2.5.29.15", "2.5.29.37");
	}

	@Test
	public void hasSubjectKeyIdentifier() {
		assertThat(dsCert.getExtensionValue("2.5.29.14")).isNotNull();
	}

	@Test
	public void authorityKeyIdentifierMatchesIacaSubjectKeyIdentifier() throws Exception {
		AuthorityKeyIdentifier aki = AuthorityKeyIdentifier.getInstance(
			JcaX509ExtensionUtils.parseExtensionValue(dsCert.getExtensionValue("2.5.29.35")));
		SubjectKeyIdentifier iacaSki = SubjectKeyIdentifier.fromExtensions(
			new X509CertificateHolder(iacaCert.getEncoded()).getExtensions());
		assertThat(aki.getKeyIdentifierOctets()).isEqualTo(iacaSki.getKeyIdentifier());
	}

	@Test
	public void issuerAlternativeNameContainsEmail() throws Exception {
		// GeneralName type 1 = rfc822Name
		assertThat(dsCert.getIssuerAlternativeNames())
			.anyMatch(name -> ((Integer) name.get(0)) == 1);
	}

	@Test
	public void hasCrlDistributionPoints() {
		assertThat(dsCert.getExtensionValue("2.5.29.31")).isNotNull();
	}

	@Test
	public void subjectMatchesIacaCountryAndStateAndHasCommonName() {
		X500Principal subject = dsCert.getSubjectX500Principal();
		String name = subject.getName(X500Principal.RFC2253);
		assertThat(name).contains("C=US");
		assertThat(name).contains("ST=State of Utopia");
		assertThat(name).contains("CN=");
	}

	@Test
	public void certChainContainsOnlyTheDsCertificate() {
		List<org.multipaz.crypto.X509Cert> chain =
			TestKeysAndCerts.getDocumentSignerKey().getCertChain().getCertificates();
		assertThat(chain).hasSize(1);
	}

	@Test
	public void iacaRootIsAValidCaForTheDsCert() {
		// sanity on the promoted root: CA:TRUE with keyCertSign, per ISO 18013-5 Table B.1
		assertThat(iacaCert.getBasicConstraints()).isGreaterThanOrEqualTo(0);
		assertThat(iacaCert.getKeyUsage()[5]).isTrue();
		Set<String> critical = iacaCert.getCriticalExtensionOIDs();
		assertThat(critical).contains("2.5.29.19", "2.5.29.15");
	}

	@Test
	public void iacaRootValidityIsWithinTableB1Limits() {
		// ISO 18013-5 Table B.1 caps IACA validity at 20 years; its note deems 9 years
		// sufficient for an mDL-only IACA, which is what this root uses
		long validityMillis = iacaCert.getNotAfter().getTime() - iacaCert.getNotBefore().getTime();
		assertThat(validityMillis).isLessThanOrEqualTo(Duration.ofDays(20 * 365 + 5).toMillis());
		assertThat(validityMillis).isLessThanOrEqualTo(Duration.ofDays(9 * 365 + 3).toMillis());
	}

	// --- MSO revocation list signer certificate, ISO/IEC 18013-5 Table B.9 ---

	@Test
	public void statusListSignerIsIssuedAndSignedByIacaRoot() throws Exception {
		// 12.3.6.2: with no Certificate element in the MSO's status element, the revocation
		// list's x5chain must chain to the certificate that signed the MSO's signer certificate
		assertThat(statusListSignerCert.getIssuerX500Principal())
			.isEqualTo(iacaCert.getSubjectX500Principal());
		statusListSignerCert.verify(iacaCert.getPublicKey());
	}

	@Test
	public void statusListSignerIsVersion3WithACsprngSerial() {
		assertThat(statusListSignerCert.getVersion()).isEqualTo(3);
		assertThat(statusListSignerCert.getSerialNumber().signum()).isEqualTo(1);
		// at least 71 bits of CSPRNG output (recommended), at most 20 octets
		assertThat(statusListSignerCert.getSerialNumber().bitLength()).isGreaterThanOrEqualTo(71);
		assertThat(statusListSignerCert.getSerialNumber().toByteArray().length).isLessThanOrEqualTo(20);
	}

	@Test
	public void statusListSignerValidityDoesNotExceed1187Days() {
		Date now = new Date();
		assertThat(statusListSignerCert.getNotBefore()).isBefore(now);
		assertThat(statusListSignerCert.getNotAfter()).isAfter(now);
		long validityMillis = statusListSignerCert.getNotAfter().getTime()
			- statusListSignerCert.getNotBefore().getTime();
		assertThat(validityMillis).isLessThanOrEqualTo(Duration.ofDays(1187).toMillis());
	}

	@Test
	public void statusListSignerKeyUsageIsCriticalAndDigitalSignatureOnly() {
		assertThat(statusListSignerCert.getCriticalExtensionOIDs()).contains("2.5.29.15");
		boolean[] keyUsage = statusListSignerCert.getKeyUsage();
		assertThat(keyUsage).isNotNull();
		assertThat(keyUsage[0]).isTrue();
		for (int i = 1; i < keyUsage.length; i++) {
			assertThat(keyUsage[i]).as("keyUsage bit %d must not be set", i).isFalse();
		}
	}

	@Test
	public void statusListSignerHasNoExtendedKeyUsageAndNoOtherCriticalExtensions() throws Exception {
		// Table B.9 makes the extended key usage optional and the OID it names
		// (oauthStatusSigning) is still TBD, so none is asserted
		assertThat(statusListSignerCert.getExtendedKeyUsage()).isNull();
		assertThat(statusListSignerCert.getCriticalExtensionOIDs()).containsExactly("2.5.29.15");
	}

	@Test
	public void statusListSignerHasSubjectKeyIdentifierAndMatchingAuthorityKeyIdentifier() throws Exception {
		assertThat(statusListSignerCert.getExtensionValue("2.5.29.14")).isNotNull();
		AuthorityKeyIdentifier aki = AuthorityKeyIdentifier.getInstance(
			JcaX509ExtensionUtils.parseExtensionValue(statusListSignerCert.getExtensionValue("2.5.29.35")));
		SubjectKeyIdentifier iacaSki = SubjectKeyIdentifier.fromExtensions(
			new X509CertificateHolder(iacaCert.getEncoded()).getExtensions());
		assertThat(aki.getKeyIdentifierOctets()).isEqualTo(iacaSki.getKeyIdentifier());
	}

	@Test
	public void statusListSignerIsNotACaAndIsDistinctFromTheDocumentSigner() {
		assertThat(statusListSignerCert.getBasicConstraints()).isEqualTo(-1);
		assertThat(statusListSignerCert.getPublicKey()).isNotEqualTo(dsCert.getPublicKey());
		assertThat(TestKeysAndCerts.getStatusListSignerKey().getCertChain().getCertificates()).hasSize(1);
	}

	private static byte[] bytes(kotlinx.io.bytestring.ByteString byteString) {
		return byteString.toByteArray(0, byteString.getSize());
	}
}
