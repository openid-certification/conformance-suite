package net.openid.conformance.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayInputStream;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each suite CA's CRL is a well-formed, empty RFC 5280 CRL signed by that CA, and a verifier
 * doing PKIX path validation with revocation checking accepts the leaf minted under the CA
 * when given it. The ISO/IEC 18013-5 Table B.10 profile details are covered for the IACA by
 * {@link MdocIacaCrl_UnitTest}; the builder is shared, so they hold for every CA.
 */
public class SuiteCaCrl_UnitTest {

	private static CertificateFactory cf;

	@BeforeAll
	public static void setUp() throws Exception {
		cf = CertificateFactory.getInstance("X.509");
	}

	@ParameterizedTest
	@EnumSource(TestKeysAndCerts.SuiteCa.class)
	public void isAnEmptyVersion2CrlSignedByTheCa(TestKeysAndCerts.SuiteCa ca) throws Exception {
		X509CRL crl = parse(SuiteCaCrl.generate(ca, Instant.parse("2026-10-14T10:15:30Z")));
		X509Certificate caCert = javaCert(ca);
		assertThat(crl.getVersion()).isEqualTo(2);
		assertThat(crl.getIssuerX500Principal()).isEqualTo(caCert.getSubjectX500Principal());
		crl.verify(caCert.getPublicKey());
		assertThat(crl.getRevokedCertificates()).isNull();
		assertThat(crl.getThisUpdate().toInstant()).isEqualTo(Instant.parse("2026-10-14T10:15:30Z"));
		assertThat(crl.getNextUpdate().toInstant()).isEqualTo(Instant.parse("2026-10-21T10:15:30Z"));
	}

	@Test
	public void leavesPassPkixValidationWithRevocationCheckingAgainstTheirCasCrl() throws Exception {
		assertLeafValidatesWithRevocation(TestKeysAndCerts.SuiteCa.PID_PROVIDER,
			TestKeysAndCerts.getSdJwtPidSigner().getCert());
		assertLeafValidatesWithRevocation(TestKeysAndCerts.SuiteCa.WALLET_PROVIDER,
			TestKeysAndCerts.getWalletAttestationSigner().getCert());
		assertLeafValidatesWithRevocation(TestKeysAndCerts.SuiteCa.ACCESS,
			TestKeysAndCerts.getWrpac().getCert());
		assertLeafValidatesWithRevocation(TestKeysAndCerts.SuiteCa.WRPRC_PROVIDER,
			TestKeysAndCerts.getWrprcSigner().getCert());
	}

	private static void assertLeafValidatesWithRevocation(TestKeysAndCerts.SuiteCa ca, org.multipaz.crypto.X509Cert leaf) throws Exception {
		X509CRL crl = parse(SuiteCaCrl.generate(ca));
		PKIXParameters params = new PKIXParameters(Set.of(new TrustAnchor(javaCert(ca), null)));
		params.setRevocationEnabled(true);
		params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(List.of(crl))));
		X509Certificate leafCert = (X509Certificate) cf.generateCertificate(
			new ByteArrayInputStream(leaf.getEncoded().toByteArray(0, leaf.getEncoded().getSize())));
		CertPathValidator.getInstance("PKIX").validate(cf.generateCertPath(List.of(leafCert)), params);
	}

	private static X509CRL parse(byte[] der) throws Exception {
		return (X509CRL) cf.generateCRL(new ByteArrayInputStream(der));
	}

	private static X509Certificate javaCert(TestKeysAndCerts.SuiteCa ca) throws Exception {
		return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(ca.getCertPem().getBytes()));
	}
}
