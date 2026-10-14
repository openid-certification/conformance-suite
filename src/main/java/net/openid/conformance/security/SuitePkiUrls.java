package net.openid.conformance.security;

import net.openid.conformance.util.TestKeysAndCerts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tells the suite's PKI ({@link TestKeysAndCerts}) where this deployment serves its CAs and their
 * CRLs, so the leaf certificates it mints point at them: the authority information
 * access pointers, the published trust anchor URLs, the registration certificate's status list and
 * the WRPAC's dNSName use the https base URL; the CRL distribution points use plain http (see
 * {@link SuiteCaCrlEndpoint}). {@code fintechlabs.external_url_override}, when set, is the origin
 * wallets and verifiers reach the suite through (a tunnel in front of a developer's machine), so
 * it is the https base URL, and the CRLs are named there too, as given: such a tunnel terminates
 * TLS and redirects plain http to https, which a CRL fetcher does not follow. Otherwise
 * {@code fintechlabs.base_http_url} names the plain-http origin where the https host does not
 * answer http on the same port, as in local deployments; when it too is empty the base URL with
 * the scheme swapped is used.
 */
@Component
public class SuitePkiUrls {

	public SuitePkiUrls(@Value("${fintechlabs.base_url}") String baseUrl,
			@Value("${fintechlabs.base_http_url:}") String baseHttpUrl,
			@Value("${fintechlabs.external_url_override:}") String externalUrlOverride) {
		TestKeysAndCerts.configureBaseUrls(httpsOrigin(baseUrl, externalUrlOverride),
			crlOrigin(baseUrl, baseHttpUrl, externalUrlOverride));
	}

	/** The origin the https URLs in the suite's certificates name; see the class comment. */
	public static String httpsOrigin(String baseUrl, String externalUrlOverride) {
		return externalUrlOverride.isBlank() ? baseUrl : externalUrlOverride;
	}

	/** The origin the CRL distribution points name; see the class comment. */
	public static String crlOrigin(String baseUrl, String baseHttpUrl, String externalUrlOverride) {
		if (!externalUrlOverride.isBlank()) {
			return externalUrlOverride;
		}
		if (!baseHttpUrl.isBlank()) {
			return baseHttpUrl;
		}
		return baseUrl.replaceFirst("^https://", "http://");
	}
}
