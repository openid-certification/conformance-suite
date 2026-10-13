package net.openid.conformance.util;

import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.multipaz.crypto.X509CertJvmKt;

import java.io.ByteArrayInputStream;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates the CRL the suite serves for its mdoc IACA against the CRL profile in
 * ISO/IEC 18013-5 Table B.10 and the issuance rules of B.2, and proves that a verifier doing
 * RFC 5280 path validation with revocation checking accepts the document signer certificate
 * when given this CRL (issue #1985).
 */
public class MdocIacaCrl_UnitTest {

	private static final Instant FIXED_NOW = Instant.parse("2026-10-13T10:15:30Z");

	private static CertificateFactory cf;
	private static X509Certificate iacaCert;
	private static X509Certificate dsCert;
	private static X509CRL crl;

	@BeforeAll
	public static void setUp() throws Exception {
		cf = CertificateFactory.getInstance("X.509");
		iacaCert = X509CertJvmKt.getJavaX509Certificate(TestKeysAndCerts.getIacaRootCert());
		dsCert = X509CertJvmKt.getJavaX509Certificate(TestKeysAndCerts.getDocumentSignerCert());
		crl = parse(MdocIacaCrl.generate(FIXED_NOW));
	}

	@Test
	public void isVersion2() {
		assertThat(crl.getVersion()).isEqualTo(2);
	}

	@Test
	public void isSignedByTheIacaWithEcdsaSha256() throws Exception {
		crl.verify(iacaCert.getPublicKey());
		assertThat(crl.getSigAlgOID()).isEqualTo("1.2.840.10045.4.3.2");
	}

	@Test
	public void issuerIsTheExactBinarySubjectOfTheIaca() {
		assertThat(crl.getIssuerX500Principal().getEncoded())
			.isEqualTo(iacaCert.getSubjectX500Principal().getEncoded());
	}

	@Test
	public void thisUpdateIsTheIssueTimeAndNextUpdateIsWithin90Days() {
		assertThat(crl.getThisUpdate()).isEqualTo(Date.from(FIXED_NOW));
		assertThat(crl.getNextUpdate()).isAfter(crl.getThisUpdate());
		assertThat(crl.getNextUpdate()).isBeforeOrEqualTo(Date.from(FIXED_NOW.plus(Duration.ofDays(90))));
	}

	@Test
	public void revokedCertificatesIsAbsentWhenNothingIsRevoked() {
		// Table B.10: shall not be present if there are no revoked certificates
		assertThat(crl.getRevokedCertificates()).isNull();
	}

	@Test
	public void authorityKeyIdentifierMatchesIacaSubjectKeyIdentifier() throws Exception {
		AuthorityKeyIdentifier aki = AuthorityKeyIdentifier.getInstance(
			JcaX509ExtensionUtils.parseExtensionValue(crl.getExtensionValue("2.5.29.35")));
		SubjectKeyIdentifier iacaSki = SubjectKeyIdentifier.fromExtensions(
			new X509CertificateHolder(iacaCert.getEncoded()).getExtensions());
		assertThat(aki.getKeyIdentifierOctets()).isEqualTo(iacaSki.getKeyIdentifier());
	}

	@Test
	public void crlNumberIncreasesWithEachIssue() throws Exception {
		X509CRL later = parse(MdocIacaCrl.generate(FIXED_NOW.plusMillis(1)));
		assertThat(crlNumber(later)).isGreaterThan(crlNumber(crl));
		assertThat(crlNumber(crl).signum()).isGreaterThanOrEqualTo(0);
	}

	@Test
	public void onlyAuthorityKeyIdentifierAndCrlNumberArePresentAndBothNonCritical() {
		// Table B.10: AKI and CRL number mandatory, non-critical; further extensions shall not be present
		assertThat(crl.getCriticalExtensionOIDs()).isNullOrEmpty();
		assertThat(crl.getNonCriticalExtensionOIDs()).containsExactlyInAnyOrder(Extension.authorityKeyIdentifier.getId(), Extension.cRLNumber.getId());
	}

	@Test
	public void documentSignerCertificatePassesPkixValidationWithRevocationCheckingAgainstThisCrl() throws Exception {
		X509CRL current = parse(MdocIacaCrl.generate(Instant.now()));
		PKIXParameters params = revocationCheckingParams();
		params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(List.of(current))));

		CertPathValidator.getInstance("PKIX").validate(dsCertPath(), params);
	}

	@Test
	public void revocationCheckingIsReallyExercisedWithoutTheCrlValidationIsUndetermined() throws Exception {
		assertThatThrownBy(() -> CertPathValidator.getInstance("PKIX").validate(dsCertPath(), revocationCheckingParams()))
			.isInstanceOf(CertPathValidatorException.class)
			.extracting(e -> ((CertPathValidatorException) e).getReason())
			.isEqualTo(CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS);
	}

	private static PKIXParameters revocationCheckingParams() throws Exception {
		PKIXParameters params = new PKIXParameters(Set.of(new TrustAnchor(iacaCert, null)));
		params.setRevocationEnabled(true);
		return params;
	}

	private static CertPath dsCertPath() throws Exception {
		return cf.generateCertPath(List.of(dsCert));
	}

	private static X509CRL parse(byte[] der) throws Exception {
		return (X509CRL) cf.generateCRL(new ByteArrayInputStream(der));
	}

	private static java.math.BigInteger crlNumber(X509CRL c) throws Exception {
		return CRLNumber.getInstance(JcaX509ExtensionUtils.parseExtensionValue(c.getExtensionValue(Extension.cRLNumber.getId())))
			.getCRLNumber();
	}
}
