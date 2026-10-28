package net.openid.conformance.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UntrustedWrpac_UnitTest {

	// the extensions whose value does not depend on the key or the issuer
	private static final List<String> PROFILE_EXTENSIONS = List.of(
		"2.5.29.15", // keyUsage
		"2.5.29.17", // subjectAltName
		"2.5.29.19", // basicConstraints (absent on both)
		"2.5.29.32", // certificatePolicies
		"2.5.29.37"  // extKeyUsage
	);

	private static String originalHttpsBaseUrl;
	private static String originalPlainHttpBaseUrl;

	private static X509Certificate wrpac;
	private static X509Certificate untrusted;
	private static X509Certificate untrustedCa;

	@BeforeAll
	public static void setUp() throws Exception {
		originalHttpsBaseUrl = TestKeysAndCerts.getHttpsBaseUrl();
		originalPlainHttpBaseUrl = TestKeysAndCerts.getPlainHttpBaseUrl();
		TestKeysAndCerts.configureBaseUrls("https://suite.example:8443", "http://suite.example:8080");
		wrpac = javaCert(TestKeysAndCerts.getWrpac().getCert().getEncoded());
		untrusted = javaCert(TestKeysAndCerts.getUntrustedWrpac().getCert().getEncoded());
		untrustedCa = TestKeysAndCerts.getUntrustedAccessCa().getJavaCert();
	}

	@AfterAll
	public static void restoreBaseUrls() {
		TestKeysAndCerts.configureBaseUrls(originalHttpsBaseUrl, originalPlainHttpBaseUrl);
	}

	@Test
	public void hasTheSubjectOfTheBuiltInAccessCertificate() {
		assertThat(untrusted.getSubjectX500Principal()).isEqualTo(wrpac.getSubjectX500Principal());
	}

	@Test
	public void hasTheExtensionsOfTheBuiltInAccessCertificate() {
		assertThat(untrusted.getCriticalExtensionOIDs()).isEqualTo(wrpac.getCriticalExtensionOIDs());
		assertThat(untrusted.getNonCriticalExtensionOIDs()).isEqualTo(wrpac.getNonCriticalExtensionOIDs());
		for (String oid : PROFILE_EXTENSIONS) {
			assertThat(untrusted.getExtensionValue(oid)).as(oid).isEqualTo(wrpac.getExtensionValue(oid));
		}
		assertThat(untrusted.getSigAlgName()).isEqualTo(wrpac.getSigAlgName());
	}

	@Test
	public void hasItsOwnKeyAndIssuer() {
		assertThat(untrusted.getPublicKey()).isNotEqualTo(wrpac.getPublicKey());
		assertThat(untrusted.getIssuerX500Principal()).isNotEqualTo(wrpac.getIssuerX500Principal());
	}

	@Test
	public void chainsToTheUntrustedCa() throws Exception {
		untrusted.verify(untrustedCa.getPublicKey());
		assertThat(untrusted.getIssuerX500Principal()).isEqualTo(untrustedCa.getSubjectX500Principal());
		validate(untrusted, Set.of(new TrustAnchor(untrustedCa, null)));
	}

	@Test
	public void doesNotChainToAnySuiteCa() throws Exception {
		Set<TrustAnchor> anchors = new HashSet<>();
		for (TestKeysAndCerts.SuiteCa ca : TestKeysAndCerts.SuiteCa.values()) {
			assertThat(ca.getJavaCert()).isNotEqualTo(untrustedCa);
			anchors.add(new TrustAnchor(ca.getJavaCert(), null));
		}
		assertThatThrownBy(() -> validate(untrusted, anchors)).isInstanceOf(CertPathValidatorException.class);
		// the built-in certificate validates against the same anchors
		validate(wrpac, anchors);
	}

	@Test
	public void doesNotChainToAnyEntityOfTheSuitesListsOfTrustedEntities() {
		Set<TrustAnchor> anchors = new HashSet<>();
		for (LoteList list : LoteList.values()) {
			for (LoteList.Service service : list.services) {
				anchors.add(new TrustAnchor(service.ca().getJavaCert(), null));
			}
		}
		assertThatThrownBy(() -> validate(untrusted, anchors)).isInstanceOf(CertPathValidatorException.class);
	}

	@Test
	public void x509HashDiffersFromTheBuiltInAccessCertificates() throws Exception {
		assertThat(x509Hash(untrusted)).isNotEqualTo(x509Hash(wrpac));
	}

	@Test
	public void twinOfAnotherCertificateHasItsSubjectAndExtensionsAndChainsToTheUntrustedCa() throws Exception {
		X509Certificate original = javaCert(TestKeysAndCerts.getWalletAttestationSigner().getCert().getEncoded());
		X509Certificate twin = javaCert(TestKeysAndCerts.mintUntrustedTwin(original).getCert().getEncoded());

		assertThat(twin.getSubjectX500Principal()).isEqualTo(original.getSubjectX500Principal());
		assertThat(twin.getCriticalExtensionOIDs()).isEqualTo(original.getCriticalExtensionOIDs());
		assertThat(twin.getNonCriticalExtensionOIDs()).isEqualTo(original.getNonCriticalExtensionOIDs());
		for (String oid : PROFILE_EXTENSIONS) {
			assertThat(twin.getExtensionValue(oid)).as(oid).isEqualTo(original.getExtensionValue(oid));
		}
		assertThat(twin.getPublicKey()).isNotEqualTo(original.getPublicKey());
		validate(twin, Set.of(new TrustAnchor(untrustedCa, null)));
	}

	@Test
	public void twinOfTheBuiltInAccessCertificateHasItsSubjectAndExtensions() throws Exception {
		X509Certificate twin = javaCert(TestKeysAndCerts.mintUntrustedTwin(wrpac).getCert().getEncoded());

		assertThat(twin.getSubjectX500Principal()).isEqualTo(wrpac.getSubjectX500Principal());
		for (String oid : PROFILE_EXTENSIONS) {
			assertThat(twin.getExtensionValue(oid)).as(oid).isEqualTo(wrpac.getExtensionValue(oid));
		}
		assertThat(twin.getExtensionValue("2.5.29.31")).isEqualTo(untrusted.getExtensionValue("2.5.29.31"));
		validate(twin, Set.of(new TrustAnchor(untrustedCa, null)));
	}

	private static void validate(X509Certificate leaf, Set<TrustAnchor> anchors) throws Exception {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		PKIXParameters params = new PKIXParameters(anchors);
		params.setRevocationEnabled(false);
		CertPathValidator.getInstance("PKIX").validate(cf.generateCertPath(List.of(leaf)), params);
	}

	private static String x509Hash(X509Certificate cert) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
		return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
	}

	private static X509Certificate javaCert(kotlinx.io.bytestring.ByteString der) throws Exception {
		return (X509Certificate) CertificateFactory.getInstance("X.509")
			.generateCertificate(new ByteArrayInputStream(der.toByteArray(0, der.getSize())));
	}
}
