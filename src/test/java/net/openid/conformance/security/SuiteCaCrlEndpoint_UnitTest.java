package net.openid.conformance.security;

import net.openid.conformance.util.MdocIacaCrl;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the /crl/{slug}.crl payloads: a DER CRL issued by the named suite CA, which the leaf
 * certificates minted under it name in their CRL distribution point. Pure unit test by repo
 * convention; anonymous and plain-http reachability of the route is covered by
 * scripts/run-security-tests.py.
 */
public class SuiteCaCrlEndpoint_UnitTest {

	private final SuiteCaCrlEndpoint endpoint = new SuiteCaCrlEndpoint();

	@ParameterizedTest
	@EnumSource(TestKeysAndCerts.SuiteCa.class)
	public void servesACrlIssuedByTheNamedCaAsPkixCrl(TestKeysAndCerts.SuiteCa ca) throws Exception {
		ResponseEntity<byte[]> response = endpoint.getCrl(ca.getSlug());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType())
			.isEqualTo(MediaType.parseMediaType(MdocIacaCrl.CONTENT_TYPE));

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509CRL crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(response.getBody()));
		X509Certificate caCert = (X509Certificate) cf.generateCertificate(
			new ByteArrayInputStream(ca.getCertPem().getBytes()));
		assertThat(crl.getIssuerX500Principal()).isEqualTo(caCert.getSubjectX500Principal());
		crl.verify(caCert.getPublicKey());
		assertThat(crl.getRevokedCertificates()).isNull();
	}

	@Test
	public void unknownNameIs404() {
		assertThat(endpoint.getCrl("no-such-ca").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
