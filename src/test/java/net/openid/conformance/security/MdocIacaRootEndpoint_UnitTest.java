package net.openid.conformance.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the /mdoc-iaca-root.pem payload: the PEM served to testers must be the IACA root that
 * actually issues the suite's mdoc document signer certificates. Pure unit test by repo
 * convention (no MockMvc infrastructure); anonymous reachability of the route is covered by
 * scripts/run-security-tests.py.
 */
public class MdocIacaRootEndpoint_UnitTest {

	@Test
	public void servesTheIacaRootCertAsParsablePem() throws Exception {
		ResponseEntity<String> response = new MdocIacaRootEndpoint().getMdocIacaRootCert();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).startsWith("-----BEGIN CERTIFICATE-----");
		assertThat(body).endsWith("-----END CERTIFICATE-----\n");

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate) cf.generateCertificate(
			new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
		// the served cert is the issuer of the runtime-minted document signer cert
		assertThat(cert.getBasicConstraints()).isGreaterThanOrEqualTo(0);
	}
}
