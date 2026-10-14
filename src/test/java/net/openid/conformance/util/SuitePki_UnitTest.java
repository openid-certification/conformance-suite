package net.openid.conformance.util;

import com.nimbusds.jose.jwk.ECKey;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.asn1.x509.PolicyQualifierId;
import org.bouncycastle.asn1.x509.PolicyQualifierInfo;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.asn1.x509.qualified.ETSIQCObjectIdentifiers;
import org.bouncycastle.asn1.x509.qualified.QCStatement;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates the suite's ETSI-profile PKI: the three CAs added for the EUDI Wallet ecosystem and
 * the leaves minted under them (and the mdoc document signer, which doubles as a PID provider
 * certificate) against ETSI TS 119 412-6 (PID and wallet provider sign/seal certificates, which
 * pulls in the EN 319 412-3 legal person subject and the EN 319 412-1 organizationIdentifier
 * syntax) and ETSI TS 119 411-8 clause 6.6.1 (wallet-relying-party access certificates), plus
 * ISO/IEC 18013-5 Table B.6 for the WRPAC's second job as an mdoc reader certificate.
 */
public class SuitePki_UnitTest {

	private static final String ORGANIZATION_IDENTIFIER_OID = "2.5.4.97";
	private static final String QCT_PID = "0.4.0.194126.1.1";
	private static final String QCT_WAL = "0.4.0.194126.1.2";
	private static final String NCP_L_EUDIWRP = "0.4.0.194118.1.2";

	private static final String HTTPS_BASE_URL = "https://suite.example:8443";
	private static final String PLAIN_HTTP_BASE_URL = "http://suite.example:8080";

	private static String originalHttpsBaseUrl;
	private static String originalPlainHttpBaseUrl;

	private static CertificateFactory cf;
	private static X509Certificate dsCert;
	private static X509Certificate sdJwtPidSignerCert;
	private static X509Certificate walletAttestationSignerCert;
	private static X509Certificate wrpacCert;

	@BeforeAll
	public static void setUp() throws Exception {
		originalHttpsBaseUrl = TestKeysAndCerts.getHttpsBaseUrl();
		originalPlainHttpBaseUrl = TestKeysAndCerts.getPlainHttpBaseUrl();
		TestKeysAndCerts.configureBaseUrls(HTTPS_BASE_URL, PLAIN_HTTP_BASE_URL);

		cf = CertificateFactory.getInstance("X.509");
		dsCert = javaCert(TestKeysAndCerts.getDocumentSignerCert());
		sdJwtPidSignerCert = javaCert(TestKeysAndCerts.getSdJwtPidSigner().getCert());
		walletAttestationSignerCert = javaCert(TestKeysAndCerts.getWalletAttestationSigner().getCert());
		wrpacCert = javaCert(TestKeysAndCerts.getWrpac().getCert());
	}

	@AfterAll
	public static void restoreBaseUrls() {
		TestKeysAndCerts.configureBaseUrls(originalHttpsBaseUrl, originalPlainHttpBaseUrl);
	}

	// --- the CAs ---

	@ParameterizedTest
	@EnumSource(TestKeysAndCerts.SuiteCa.class)
	public void caIsASelfSignedCaWithKeyCertSignAndCrlSignAndASubjectKeyIdentifier(TestKeysAndCerts.SuiteCa ca) throws Exception {
		X509Certificate cert = javaCert(ca.getCert());
		cert.verify(cert.getPublicKey());
		assertThat(cert.getIssuerX500Principal()).isEqualTo(cert.getSubjectX500Principal());
		assertThat(cert.getBasicConstraints()).isEqualTo(0);
		assertThat(cert.getCriticalExtensionOIDs()).contains("2.5.29.19", "2.5.29.15");
		boolean[] keyUsage = cert.getKeyUsage();
		assertThat(keyUsage[5]).as("keyCertSign").isTrue();
		assertThat(keyUsage[6]).as("cRLSign").isTrue();
		assertThat(cert.getExtensionValue("2.5.29.14")).isNotNull();
		assertThat(cert.getNotAfter()).isAfter(new Date());
	}

	@ParameterizedTest
	@EnumSource(TestKeysAndCerts.SuiteCa.class)
	public void caSubjectNamesTheOrganization(TestKeysAndCerts.SuiteCa ca) throws Exception {
		// TS 119 602 6.6.3: a listed certificate should carry the organizationName matching the TEName
		X500Name subject = subject(javaCert(ca.getCert()));
		assertThat(rdnValue(subject, BCStyle.O)).isEqualTo(TestKeysAndCerts.ORGANIZATION_NAME);
		assertThat(rdnValue(subject, BCStyle.C)).isEqualTo("US");
		assertThat(rdnValue(subject, BCStyle.CN)).isNotEmpty();
	}

	@ParameterizedTest
	@EnumSource(value = TestKeysAndCerts.SuiteCa.class, names = {"PID_PROVIDER", "WALLET_PROVIDER", "ACCESS"})
	public void etsiCaSubjectCarriesTheOrganizationIdentifier(TestKeysAndCerts.SuiteCa ca) throws Exception {
		// EN 319 412-2 GEN-4.2.3.1-3: an issuer with a known registration number carries it
		assertThat(rdnValue(subject(javaCert(ca.getCert())), new ASN1ObjectIdentifier(ORGANIZATION_IDENTIFIER_OID)))
			.isEqualTo(TestKeysAndCerts.ORGANIZATION_IDENTIFIER);
	}

	@Test
	public void theIacaRootIsThePublishedCertificateAndMatchesItsCommittedCopy() throws Exception {
		TestKeysAndCerts.SuiteCa iaca = TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT;
		assertThat(iaca.getIdentity().getEphemeral()).isFalse();
		assertThat(iaca.getCertPem()).isEqualTo(iaca.getPublishedCertPem());
		assertThat(Files.readString(Path.of("scripts", "certs-keys", "mdoc-iaca-root.crt"), StandardCharsets.UTF_8).strip())
			.isEqualTo(iaca.getPublishedCertPem().strip());
	}

	@ParameterizedTest
	@EnumSource(value = TestKeysAndCerts.SuiteCa.class, names = {"PID_PROVIDER", "WALLET_PROVIDER", "ACCESS"})
	public void withoutAKeyDirectoryAnEphemeralCaStandsInForThePublishedOne(TestKeysAndCerts.SuiteCa ca) throws Exception {
		TestKeysAndCerts.SigningIdentity identity = TestKeysAndCerts.resolveIdentity(ca.getSlug(), ca.getPublishedCertPem(), null, true, null);
		assertThat(identity.getEphemeral()).isTrue();
		X509Certificate published = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(ca.getPublishedCertPem().getBytes(StandardCharsets.UTF_8)));
		assertThat(identity.getJavaCert().getPublicKey()).isNotEqualTo(published.getPublicKey());
		assertThat(identity.getJavaCert().getSubjectX500Principal().getName()).contains("(ephemeral)");
		assertThat(rdnValue(subject(identity.getJavaCert()), BCStyle.O)).isEqualTo(TestKeysAndCerts.ORGANIZATION_NAME);
		assertThat(identity.getJavaCert().getBasicConstraints()).isEqualTo(0);
	}

	@Test
	public void aMatchingBase64EncodedPemKeyMakesThePublishedCertificateTheOneInUse() throws Exception {
		// stand in for a published certificate whose key the deployment holds
		TestKeysAndCerts.SigningIdentity minted = TestKeysAndCerts.resolveIdentity("test-ca", TestKeysAndCerts.PID_PROVIDER_CA_CERT_PEM, null, true, null);
		String pem = "-----BEGIN PRIVATE KEY-----\n"
			+ Base64.getMimeEncoder().encodeToString(minted.getPrivateKey().getEncoded()) + "\n-----END PRIVATE KEY-----\n";

		TestKeysAndCerts.SigningIdentity loaded = TestKeysAndCerts.resolveIdentity("test-ca", minted.getCertPem(), null, true,
			Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.US_ASCII)));

		assertThat(loaded.getEphemeral()).isFalse();
		assertThat(loaded.getCertPem()).isEqualTo(minted.getCertPem());
		assertThat(loaded.getPrivateKey().getEncoded()).isEqualTo(minted.getPrivateKey().getEncoded());
	}

	@Test
	public void aMatchingBase64EncodedDerKeyIsAcceptedToo() throws Exception {
		TestKeysAndCerts.SigningIdentity minted = TestKeysAndCerts.resolveIdentity("test-ca", TestKeysAndCerts.PID_PROVIDER_CA_CERT_PEM, null, true, null);

		TestKeysAndCerts.SigningIdentity loaded = TestKeysAndCerts.resolveIdentity("test-ca", minted.getCertPem(), null, true,
			Base64.getEncoder().encodeToString(minted.getPrivateKey().getEncoded()));

		assertThat(loaded.getEphemeral()).isFalse();
	}

	@Test
	public void aKeyThatDoesNotBelongToThePublishedCertificateIsRefused() throws Exception {
		TestKeysAndCerts.SigningIdentity other = TestKeysAndCerts.resolveIdentity("other", TestKeysAndCerts.PID_PROVIDER_CA_CERT_PEM, null, true, null);

		assertThatThrownBy(() -> TestKeysAndCerts.resolveIdentity("pid-provider-ca", TestKeysAndCerts.PID_PROVIDER_CA_CERT_PEM, null, true,
				Base64.getEncoder().encodeToString(other.getPrivateKey().getEncoded())))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("SUITE_CA_KEY_PID_PROVIDER_CA is not the private key of the published pid-provider-ca certificate");
	}

	@Test
	public void aValueThatIsNotBase64IsRefused() {
		assertThatThrownBy(() -> TestKeysAndCerts.resolveIdentity("access-ca", TestKeysAndCerts.ACCESS_CA_CERT_PEM, null, true, "not base64!"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("SUITE_CA_KEY_ACCESS_CA is not base64");
	}

	@Test
	public void aBlankValueFallsBackToAnEphemeralCa() {
		// the deploy job creates the secret even when a CI/CD variable is unset, so its value is empty
		TestKeysAndCerts.SigningIdentity identity = TestKeysAndCerts.resolveIdentity("access-ca", TestKeysAndCerts.ACCESS_CA_CERT_PEM, null, true, "");
		assertThat(identity.getEphemeral()).isTrue();
	}

	@Test
	public void eachCasKeyHasItsOwnEnvironmentVariable() {
		assertThat(TestKeysAndCerts.keyEnvironmentVariable("wrprc-provider-ca")).isEqualTo("SUITE_CA_KEY_WRPRC_PROVIDER_CA");
		assertThat(TestKeysAndCerts.keyEnvironmentVariable("lote-signer")).isEqualTo("SUITE_CA_KEY_LOTE_SIGNER");
	}

	@ParameterizedTest
	@EnumSource(TestKeysAndCerts.SuiteCa.class)
	public void caUrlsAreUnderTheConfiguredBaseUrls(TestKeysAndCerts.SuiteCa ca) {
		assertThat(ca.getTrustAnchorUrl()).isEqualTo(HTTPS_BASE_URL + "/trust-anchors/" + ca.getSlug() + ".pem");
		assertThat(ca.getCaIssuersUrl()).isEqualTo(HTTPS_BASE_URL + "/trust-anchors/" + ca.getSlug() + ".crt");
		assertThat(ca.getCrlUrl()).startsWith(PLAIN_HTTP_BASE_URL + "/");
		assertThat(ca.getCrlUrl()).endsWith(".crl");
	}

	@Test
	public void organizationIdentifierFollowsTheLeiSyntaxOfEn319412_1() {
		// LEG-5.1.4-02/-03 item 4: "LEI", country code "XG", a hyphen, then the 20-character LEI
		assertThat(TestKeysAndCerts.ORGANIZATION_IDENTIFIER).matches("LEIXG-[0-9A-Z]{20}");
	}

	// --- common leaf checks ---

	@Test
	public void everyLeafIsIssuedBySignedByAndChainsToItsCa() throws Exception {
		assertIssuedBy(dsCert, TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT);
		assertIssuedBy(sdJwtPidSignerCert, TestKeysAndCerts.SuiteCa.PID_PROVIDER);
		assertIssuedBy(walletAttestationSignerCert, TestKeysAndCerts.SuiteCa.WALLET_PROVIDER);
		assertIssuedBy(wrpacCert, TestKeysAndCerts.SuiteCa.ACCESS);
	}

	@Test
	public void everyLeafHasTheLegalPersonSubjectOfEn319412_3() {
		// LEG-4.2.1-2: countryName, organizationName, organizationIdentifier and commonName, once each
		for (X509Certificate cert : List.of(dsCert, sdJwtPidSignerCert, walletAttestationSignerCert, wrpacCert)) {
			X500Name subject = subject(cert);
			assertThat(rdnValue(subject, BCStyle.C)).isEqualTo("US");
			assertThat(rdnValue(subject, BCStyle.O)).isEqualTo(TestKeysAndCerts.ORGANIZATION_NAME);
			assertThat(rdnValue(subject, new ASN1ObjectIdentifier(ORGANIZATION_IDENTIFIER_OID)))
				.isEqualTo(TestKeysAndCerts.ORGANIZATION_IDENTIFIER);
			assertThat(rdnValue(subject, BCStyle.CN)).isNotEmpty();
			for (ASN1ObjectIdentifier type : List.of(BCStyle.C, BCStyle.O, BCStyle.CN, new ASN1ObjectIdentifier(ORGANIZATION_IDENTIFIER_OID))) {
				assertThat(subject.getRDNs(type)).as("one instance of %s", type).hasSize(1);
			}
		}
	}

	@Test
	public void everyLeafHasDigitalSignatureOnlyKeyUsageAndNoBasicConstraints() {
		// EN 319 412-2 Table 1 type C, which TS 119 412-6 PID-4.4.1-02 allows; ISO/IEC 18013-5 Annex B
		for (X509Certificate cert : List.of(dsCert, sdJwtPidSignerCert, walletAttestationSignerCert, wrpacCert)) {
			assertThat(cert.getCriticalExtensionOIDs()).contains("2.5.29.15");
			boolean[] keyUsage = cert.getKeyUsage();
			assertThat(keyUsage[0]).isTrue();
			for (int i = 1; i < keyUsage.length; i++) {
				assertThat(keyUsage[i]).as("keyUsage bit %d", i).isFalse();
			}
			assertThat(cert.getBasicConstraints()).isEqualTo(-1);
		}
	}

	@Test
	public void everyLeafPointsAtItsCaThroughAuthorityInformationAccess() throws Exception {
		// PID-4.4.3-01..03: AIA with id-ad-caIssuers and an http(s) location, non-critical
		assertCaIssuersUrl(dsCert, TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT);
		assertCaIssuersUrl(sdJwtPidSignerCert, TestKeysAndCerts.SuiteCa.PID_PROVIDER);
		assertCaIssuersUrl(walletAttestationSignerCert, TestKeysAndCerts.SuiteCa.WALLET_PROVIDER);
		assertCaIssuersUrl(wrpacCert, TestKeysAndCerts.SuiteCa.ACCESS);
	}

	@Test
	public void everyLeafNamesItsCasCrlInItsCrlDistributionPoint() throws Exception {
		assertThat(crlDistributionPointUri(dsCert)).isEqualTo(PLAIN_HTTP_BASE_URL + "/mdoc-iaca.crl");
		assertThat(crlDistributionPointUri(sdJwtPidSignerCert)).isEqualTo(PLAIN_HTTP_BASE_URL + "/crl/pid-provider-ca.crl");
		assertThat(crlDistributionPointUri(walletAttestationSignerCert)).isEqualTo(PLAIN_HTTP_BASE_URL + "/crl/wallet-provider-ca.crl");
		assertThat(crlDistributionPointUri(wrpacCert)).isEqualTo(PLAIN_HTTP_BASE_URL + "/crl/access-ca.crl");
	}

	@Test
	public void everyLeafHasSubjectKeyIdentifierAndAuthorityKeyIdentifierMatchingItsCa() throws Exception {
		assertAkiMatchesCa(dsCert, TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT);
		assertAkiMatchesCa(sdJwtPidSignerCert, TestKeysAndCerts.SuiteCa.PID_PROVIDER);
		assertAkiMatchesCa(walletAttestationSignerCert, TestKeysAndCerts.SuiteCa.WALLET_PROVIDER);
		assertAkiMatchesCa(wrpacCert, TestKeysAndCerts.SuiteCa.ACCESS);
	}

	@Test
	public void everyLeafIsCurrentlyValidForAtMost457Days() {
		Date now = new Date();
		for (X509Certificate cert : List.of(dsCert, sdJwtPidSignerCert, walletAttestationSignerCert, wrpacCert)) {
			assertThat(cert.getNotBefore()).isBefore(now);
			assertThat(cert.getNotAfter()).isAfter(now);
			assertThat(cert.getNotAfter().getTime() - cert.getNotBefore().getTime())
				.isLessThanOrEqualTo(Duration.ofDays(457).toMillis());
		}
	}

	@Test
	public void everyMintedLeafExposesAPrivateJwkWhoseX5cIsTheCertificate() throws Exception {
		for (TestKeysAndCerts.MintedLeaf leaf : List.of(TestKeysAndCerts.getSdJwtPidSigner(),
				TestKeysAndCerts.getWalletAttestationSigner(), TestKeysAndCerts.getWrpac())) {
			ECKey jwk = leaf.getJwk();
			assertThat(jwk.isPrivate()).isTrue();
			assertThat(jwk.getKeyID()).isNotBlank();
			assertThat(jwk.getX509CertChain()).hasSize(1);
			assertThat(jwk.getX509CertChain().get(0).decode()).isEqualTo(bytes(leaf.getCert().getEncoded()));
			assertThat(jwk.toECPublicKey()).isEqualTo(javaCert(leaf.getCert()).getPublicKey());
			assertThat(leaf.jwksJson().getAsJsonArray("keys")).hasSize(1);
			assertThat(leaf.jwksJson().getAsJsonArray("keys").get(0).getAsJsonObject().has("d")).isTrue();
		}
	}

	// --- PID provider and wallet provider sign/seal certificates, TS 119 412-6 clauses 4 and 5 ---

	@Test
	public void pidSignersCarryTheQcTypeForAPidProvider() throws Exception {
		// PID-4.5-01: QcType qcStatement with id-etsi-qct-pid; the mdoc DS signs mdoc PIDs
		assertThat(qcTypes(dsCert)).containsExactly(QCT_PID);
		assertThat(qcTypes(sdJwtPidSignerCert)).containsExactly(QCT_PID);
	}

	@Test
	public void walletAttestationSignerCarriesTheQcTypeForAWalletProvider() throws Exception {
		// WAL-5.2-01
		assertThat(qcTypes(walletAttestationSignerCert)).containsExactly(QCT_WAL);
	}

	@Test
	public void sdJwtPidSignerAndWalletAttestationSignerHaveNoExtendedKeyUsageAndOnlyKeyUsageCritical() throws Exception {
		// PID-4.1-02: no critical extensions beyond what RFC 5280 allows; nothing calls for an EKU
		for (X509Certificate cert : List.of(sdJwtPidSignerCert, walletAttestationSignerCert)) {
			assertThat(cert.getExtendedKeyUsage()).isNull();
			assertThat(cert.getCriticalExtensionOIDs()).containsExactly("2.5.29.15");
		}
	}

	@Test
	public void pidAndWalletSignersCarryTheNcpPolicyWithACpsPointer() throws Exception {
		// EN 319 412-2 GEN-4.3.3-2: certificatePolicies present; EN 319 411-1 NCP, with the CPS as qualifier
		for (X509Certificate cert : List.of(dsCert, sdJwtPidSignerCert, walletAttestationSignerCert)) {
			CertificatePolicies policies = CertificatePolicies.getInstance(
				JcaX509ExtensionUtils.parseExtensionValue(cert.getExtensionValue("2.5.29.32")));
			assertThat(policies.getPolicyInformation()).hasSize(1);
			PolicyInformation policy = policies.getPolicyInformation()[0];
			assertThat(policy.getPolicyIdentifier().getId()).isEqualTo("0.4.0.2042.1.1");
			assertThat(cpsUris(policy)).containsExactly(TestKeysAndCerts.CONTACT_URL);
			assertThat(cert.getNonCriticalExtensionOIDs()).contains("2.5.29.32");
		}
	}

	@Test
	public void qcStatementsExtensionIsNotCritical() {
		for (X509Certificate cert : List.of(dsCert, sdJwtPidSignerCert, walletAttestationSignerCert)) {
			assertThat(cert.getNonCriticalExtensionOIDs()).contains("1.3.6.1.5.5.7.1.3");
		}
	}

	// --- wallet-relying-party access certificate, TS 119 411-8 clause 6.6.1 and ISO/IEC 18013-5 Table B.6 ---

	@Test
	public void wrpacCarriesTheNcpLegalPersonPolicyWithACpsPointer() throws Exception {
		// GEN-6.6.1-03 (NCP-l-eudiwrp, clause 5.3 item 2) and GEN-6.6.1-06 (cpsURI)
		CertificatePolicies policies = CertificatePolicies.getInstance(
			JcaX509ExtensionUtils.parseExtensionValue(wrpacCert.getExtensionValue("2.5.29.32")));
		assertThat(policies.getPolicyInformation()).hasSize(1);
		PolicyInformation policy = policies.getPolicyInformation()[0];
		assertThat(policy.getPolicyIdentifier().getId()).isEqualTo(NCP_L_EUDIWRP);
		assertThat(cpsUris(policy)).containsExactly(TestKeysAndCerts.CONTACT_URL);
		assertThat(wrpacCert.getNonCriticalExtensionOIDs()).contains("2.5.29.32");
	}

	private static List<String> cpsUris(PolicyInformation policy) {
		List<String> cpsUris = new ArrayList<>();
		for (var qualifier : ASN1Sequence.getInstance(policy.getPolicyQualifiers())) {
			PolicyQualifierInfo info = PolicyQualifierInfo.getInstance(qualifier);
			if (PolicyQualifierId.id_qt_cps.equals(info.getPolicyQualifierId())) {
				cpsUris.add(info.getQualifier().toString());
			}
		}
		return cpsUris;
	}

	@Test
	public void wrpacSubjectAlternativeNameCarriesContactDetailsAndTheBaseUrlHost() throws Exception {
		// GEN-6.6.1-07: contact as a URI, telephone otherName or rfc822Name; dNSName for x509_san_dns
		Collection<List<?>> sans = wrpacCert.getSubjectAlternativeNames();
		assertThat(sans).isNotNull();
		List<String> dnsNames = new ArrayList<>();
		List<String> emails = new ArrayList<>();
		List<String> uris = new ArrayList<>();
		for (List<?> san : sans) {
			switch ((Integer) san.get(0)) {
				case GeneralName.dNSName -> dnsNames.add((String) san.get(1));
				case GeneralName.rfc822Name -> emails.add((String) san.get(1));
				case GeneralName.uniformResourceIdentifier -> uris.add((String) san.get(1));
				default -> { }
			}
		}
		assertThat(dnsNames).containsExactly("suite.example");
		assertThat(emails).hasSize(1);
		assertThat(emails.get(0)).contains("@");
		assertThat(uris).hasSize(1);
		assertThat(uris.get(0)).startsWith("https://");
	}

	@Test
	public void wrpacHasCriticalMdocReaderAuthenticationExtendedKeyUsages() throws Exception {
		assertThat(wrpacCert.getCriticalExtensionOIDs()).containsExactlyInAnyOrder("2.5.29.15", "2.5.29.37");
		assertThat(wrpacCert.getExtendedKeyUsage()).containsExactlyInAnyOrder("1.0.18013.5.1.6", "1.0.23220.4.1.6");
	}

	@Test
	public void wrpacCarriesNoQcType() {
		// an access certificate is neither a PID provider's nor a wallet provider's sign/seal certificate
		assertThat(wrpacCert.getExtensionValue("1.3.6.1.5.5.7.1.3")).isNull();
	}

	@Test
	public void wrpacSatisfiesTheIsoReaderCertificateChecksTheSuiteAppliesToVerifiers() throws Exception {
		List<String> violations = new ArrayList<>();
		MdocCertificateProfileChecks.checkVersionAndSerial(wrpacCert, violations);
		MdocCertificateProfileChecks.checkValidity(wrpacCert, 1187, violations);
		MdocCertificateProfileChecks.checkSignatureAlgorithm(wrpacCert, violations);
		MdocCertificateProfileChecks.checkSubjectAttributes(wrpacCert, violations);
		MdocCertificateProfileChecks.checkSubjectPublicKey(wrpacCert, false, violations);
		MdocCertificateProfileChecks.checkSubjectKeyIdentifierValue(wrpacCert, violations);
		MdocCertificateProfileChecks.checkDigitalSignatureOnlyKeyUsage(wrpacCert, "Table B.6", violations);
		MdocCertificateProfileChecks.checkCrlDistributionPointsContent(wrpacCert, violations);
		assertThat(violations).isEmpty();
	}

	// --- rotation ---

	@Test
	public void changingTheBaseUrlsReMintsEveryLeaf() throws Exception {
		TestKeysAndCerts.configureBaseUrls("https://other.example", "http://other.example");
		try {
			X509Certificate reMintedWrpac = javaCert(TestKeysAndCerts.getWrpac().getCert());
			assertThat(reMintedWrpac.getSerialNumber()).isNotEqualTo(wrpacCert.getSerialNumber());
			assertThat(reMintedWrpac.getSubjectAlternativeNames()).anyMatch(san -> "other.example".equals(san.get(1)));
			assertThat(crlDistributionPointUri(javaCert(TestKeysAndCerts.getSdJwtPidSigner().getCert())))
				.isEqualTo("http://other.example/crl/pid-provider-ca.crl");
			assertThat(javaCert(TestKeysAndCerts.getWalletAttestationSigner().getCert()).getSerialNumber())
				.isNotEqualTo(walletAttestationSignerCert.getSerialNumber());
			assertThat(javaCert(TestKeysAndCerts.getDocumentSignerCert()).getSerialNumber())
				.isNotEqualTo(dsCert.getSerialNumber());
		} finally {
			TestKeysAndCerts.configureBaseUrls(HTTPS_BASE_URL, PLAIN_HTTP_BASE_URL);
		}
	}

	// --- helpers ---

	private static void assertIssuedBy(X509Certificate leaf, TestKeysAndCerts.SuiteCa ca) throws Exception {
		X509Certificate caCert = javaCert(ca.getCert());
		assertThat(leaf.getIssuerX500Principal()).isEqualTo(caCert.getSubjectX500Principal());
		leaf.verify(caCert.getPublicKey());
	}

	private static void assertCaIssuersUrl(X509Certificate leaf, TestKeysAndCerts.SuiteCa ca) throws Exception {
		byte[] value = leaf.getExtensionValue("1.3.6.1.5.5.7.1.1");
		assertThat(value).isNotNull();
		assertThat(leaf.getNonCriticalExtensionOIDs()).contains("1.3.6.1.5.5.7.1.1");
		AuthorityInformationAccess aia = AuthorityInformationAccess.getInstance(JcaX509ExtensionUtils.parseExtensionValue(value));
		assertThat(aia.getAccessDescriptions()).hasSize(1);
		AccessDescription description = aia.getAccessDescriptions()[0];
		assertThat(description.getAccessMethod()).isEqualTo(X509ObjectIdentifiers.id_ad_caIssuers);
		assertThat(description.getAccessLocation().getTagNo()).isEqualTo(GeneralName.uniformResourceIdentifier);
		assertThat(description.getAccessLocation().getName().toString()).isEqualTo(ca.getCaIssuersUrl());
	}

	private static void assertAkiMatchesCa(X509Certificate leaf, TestKeysAndCerts.SuiteCa ca) throws Exception {
		assertThat(leaf.getExtensionValue("2.5.29.14")).isNotNull();
		AuthorityKeyIdentifier aki = AuthorityKeyIdentifier.getInstance(
			JcaX509ExtensionUtils.parseExtensionValue(leaf.getExtensionValue("2.5.29.35")));
		SubjectKeyIdentifier caSki = SubjectKeyIdentifier.fromExtensions(ca.getHolder().getExtensions());
		assertThat(aki.getKeyIdentifierOctets()).isEqualTo(caSki.getKeyIdentifier());
	}

	private static List<String> qcTypes(X509Certificate cert) throws Exception {
		byte[] value = cert.getExtensionValue("1.3.6.1.5.5.7.1.3");
		assertThat(value).isNotNull();
		List<String> types = new ArrayList<>();
		for (var statementEl : ASN1Sequence.getInstance(JcaX509ExtensionUtils.parseExtensionValue(value))) {
			QCStatement statement = QCStatement.getInstance(statementEl);
			assertThat(statement.getStatementId()).isEqualTo(ETSIQCObjectIdentifiers.id_etsi_qcs_QcType);
			for (var oid : ASN1Sequence.getInstance(statement.getStatementInfo())) {
				types.add(ASN1ObjectIdentifier.getInstance(oid).getId());
			}
		}
		return types;
	}

	private static String crlDistributionPointUri(X509Certificate cert) throws Exception {
		CRLDistPoint crldp = CRLDistPoint.getInstance(
			JcaX509ExtensionUtils.parseExtensionValue(cert.getExtensionValue("2.5.29.31")));
		assertThat(crldp.getDistributionPoints()).hasSize(1);
		GeneralName[] names = GeneralNames.getInstance(
			crldp.getDistributionPoints()[0].getDistributionPoint().getName()).getNames();
		assertThat(names).hasSize(1);
		return names[0].getName().toString();
	}

	private static X500Name subject(X509Certificate cert) {
		return X500Name.getInstance(cert.getSubjectX500Principal().getEncoded());
	}

	private static String rdnValue(X500Name name, ASN1ObjectIdentifier type) {
		RDN[] rdns = name.getRDNs(type);
		assertThat(rdns).as("attribute %s", type).isNotEmpty();
		return rdns[0].getFirst().getValue().toString();
	}

	private static X509Certificate javaCert(org.multipaz.crypto.X509Cert cert) throws Exception {
		return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes(cert.getEncoded())));
	}

	private static byte[] bytes(kotlinx.io.bytestring.ByteString byteString) {
		return byteString.toByteArray(0, byteString.getSize());
	}
}
