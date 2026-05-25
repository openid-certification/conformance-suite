package net.openid.conformance.vci10issuer.condition;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Validates an RFC 8414 §2 issuer-identifier URL: it must be a syntactically valid URI using the
 * {@code https} scheme (case-insensitive per RFC 3986 §3.1), have a host component, and carry no
 * fragment, query, userinfo or out-of-range port.
 *
 * <p>Shared by {@link VCIValidateCredentialIssuerUri} (the {@code credential_issuer} identifier,
 * OID4VCI 1.0 Final §12.2.1) and {@link VCIValidateAuthorizationServersAreHttps} (each
 * {@code authorization_servers} entry, §12.2.3) so both apply identical rules.
 */
final class VciIssuerUrlValidation {

	private VciIssuerUrlValidation() {
	}

	/**
	 * Appends one issue per problem found with {@code value} to {@code issues}.
	 *
	 * @param value  the URL to validate
	 * @param label  the field label used to prefix each issue message, e.g. {@code "credential_issuer"}
	 *               or {@code "authorization_servers[0]"}
	 * @param issues collector for human-readable problem descriptions
	 */
	static void validate(String value, String label, List<String> issues) {
		URI uri;
		try {
			uri = new URI(value);
		} catch (URISyntaxException e) {
			issues.add(String.format("%s: '%s' is not a valid URI (%s)", label, value, e.getMessage()));
			return;
		}
		if (uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) {
			issues.add(String.format("%s: '%s' must use the https scheme", label, value));
		}
		if (uri.getHost() == null || uri.getHost().isEmpty()) {
			issues.add(String.format("%s: '%s' is missing a host component", label, value));
		}
		if (uri.getFragment() != null) {
			issues.add(String.format("%s: '%s' must not contain a fragment part", label, value));
		}
		if (uri.getQuery() != null) {
			issues.add(String.format("%s: '%s' must not contain a query part", label, value));
		}
		if (uri.getUserInfo() != null) {
			issues.add(String.format("%s: '%s' must not contain userinfo", label, value));
		}
		// URI.getPort() returns -1 when absent or a non-negative int otherwise, so only the upper
		// bound can be violated.
		if (uri.getPort() > 65535) {
			issues.add(String.format("%s: '%s' contains an out-of-range port (%d)", label, value, uri.getPort()));
		}
	}
}
