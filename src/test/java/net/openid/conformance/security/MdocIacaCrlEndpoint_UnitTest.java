package net.openid.conformance.security;

import net.openid.conformance.util.MdocIacaCrl;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the /mdoc-iaca.crl payload: a DER CRL issued by the IACA root that the suite's document
 * signer certificates name in their CRL distribution point. Pure unit test by repo convention
 * (no MockMvc infrastructure); anonymous reachability of the route is covered by
 * scripts/run-security-tests.py.
 */
public class MdocIacaCrlEndpoint_UnitTest {

	@Test
	public void servesACrlIssuedByTheIacaRootAsPkixCrl() throws Exception {
		ResponseEntity<byte[]> response = new MdocIacaCrlEndpoint().getMdocIacaCrl();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType())
			.isEqualTo(MediaType.parseMediaType(MdocIacaCrl.CONTENT_TYPE));

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509CRL crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(response.getBody()));
		X509Certificate iaca = (X509Certificate) cf.generateCertificate(
			new ByteArrayInputStream(TestKeysAndCerts.IACA_ROOT_CERT_PEM.getBytes()));
		assertThat(crl.getIssuerX500Principal()).isEqualTo(iaca.getSubjectX500Principal());
		crl.verify(iaca.getPublicKey());
	}
}
