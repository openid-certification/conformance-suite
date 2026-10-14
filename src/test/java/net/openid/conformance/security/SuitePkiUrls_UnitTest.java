package net.openid.conformance.security;

import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the URLs {@link SuitePkiUrls} configures into the suite's PKI: the plain-http CRL
 * distribution point of the document signer and the https URLs of the trust anchors.
 */
public class SuitePkiUrls_UnitTest {

	@Test
	public void pointsTheDocumentSignerAtTheCrlOverPlainHttp() {
		new SuitePkiUrls("https://x.example:8443", "http://x.example:8080", "");
		assertThat(TestKeysAndCerts.getCrlDistributionPointUri()).isEqualTo("http://x.example:8080/mdoc-iaca.crl");
	}

	@Test
	public void derivesThePlainHttpUrlFromTheBaseUrlWhenNoneIsConfigured() {
		new SuitePkiUrls("https://x.example:8443", "", "");
		assertThat(TestKeysAndCerts.getCrlDistributionPointUri()).isEqualTo("http://x.example:8443/mdoc-iaca.crl");
	}

	@Test
	public void dropsATrailingSlashFromTheOrigin() {
		new SuitePkiUrls("https://x.example:8443/", "http://x.example:8080/", "");
		assertThat(TestKeysAndCerts.getCrlDistributionPointUri()).isEqualTo("http://x.example:8080/mdoc-iaca.crl");
	}

	@Test
	public void prefersTheExternalUrlOverrideAsGiven() {
		// a tunnel redirects plain http to https, which a CRL fetcher does not follow
		new SuitePkiUrls("https://x.example:8443", "http://x.example:8080", "https://tunnel.example");
		assertThat(TestKeysAndCerts.getCrlDistributionPointUri()).isEqualTo("https://tunnel.example/mdoc-iaca.crl");
	}

	@Test
	public void namesTheExternalUrlOverrideInTheHttpsUrls() {
		// wallets reaching the suite through a tunnel fetch the trust anchors and status lists there
		new SuitePkiUrls("https://x.example:8443", "http://x.example:8080", "https://tunnel.example");
		assertThat(TestKeysAndCerts.getHttpsBaseUrl()).isEqualTo("https://tunnel.example");
		assertThat(TestKeysAndCerts.SuiteCa.ACCESS.getTrustAnchorUrl())
			.isEqualTo("https://tunnel.example/trust-anchors/access-ca.pem");
		assertThat(TestKeysAndCerts.getWrpacDnsName()).isEqualTo("tunnel.example");
	}

	@AfterEach
	public void restoreBaseUrls() {
		TestKeysAndCerts.configureBaseUrls(ORIGINAL_HTTPS_BASE_URL, ORIGINAL_PLAIN_HTTP_BASE_URL);
	}

	private static final String ORIGINAL_HTTPS_BASE_URL = TestKeysAndCerts.getHttpsBaseUrl();
	private static final String ORIGINAL_PLAIN_HTTP_BASE_URL = TestKeysAndCerts.getPlainHttpBaseUrl();
}
