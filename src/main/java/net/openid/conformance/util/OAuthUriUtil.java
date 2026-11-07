package net.openid.conformance.util;

import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class OAuthUriUtil {

	/**
	 * Appends request parameters to an endpoint URL as an application/x-www-form-urlencoded query
	 * (RFC 6749 section 4.1.1 and appendix B). Every character of a name or value other than the
	 * RFC 3986 unreserved set is percent-encoded, so a '+' is sent as "%2B" and a space as "%20":
	 * form decoders read a literal '+' as a space, and generic URI decoders do not recognize '+' as
	 * one, so this is the only encoding both read back identically. A query already present on the
	 * endpoint URL is retained (RFC 6749 section 3.1), and the parameters go before any fragment. A null
	 * value is sent as a bare name. The URL itself is not parsed: wallet endpoints such as
	 * "openid4vp://" (OID4VP 1.0 section 13.1.2) are not valid java.net.URI values.
	 */
	public static String appendFormUrlEncodedQuery(String url, Map<String, String> parameters) {
		int fragmentStart = url.indexOf('#');
		String beforeFragment = fragmentStart == -1 ? url : url.substring(0, fragmentStart);
		String fragment = fragmentStart == -1 ? "" : url.substring(fragmentStart);
		StringBuilder sb = new StringBuilder(beforeFragment);
		char separator = beforeFragment.indexOf('?') == -1 ? '?' : '&';
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			sb.append(separator);
			separator = '&';
			sb.append(formUrlEncode(entry.getKey()));
			if (entry.getValue() != null) {
				sb.append('=');
				sb.append(formUrlEncode(entry.getValue()));
			}
		}
		sb.append(fragment);
		return sb.toString();
	}

	private static String formUrlEncode(String value) {
		return UriUtils.encode(value, StandardCharsets.UTF_8);
	}

	/**
	 * Builds well-known metadata URLs for specs using the RFC 8414-style path
	 * insertion rule. This intentionally uses only {@link URI#getPath()}; callers
	 * that need query-component preservation must extend this helper before using it.
	 */
	public static String generateWellKnownUrlForPath(String issuer, String wellKnownTypePathComponent) {

		URI serverIssuerUri = URI.create(issuer);

		String portComponent = "";
		int port = serverIssuerUri.getPort();
		if (port != -1) {
			portComponent = ":" + port;
		}
		String wellKnownBaseUrl = serverIssuerUri.getScheme() + "://" + serverIssuerUri.getHost() + portComponent + "/.well-known";

		String newUrl = wellKnownBaseUrl + "/" + wellKnownTypePathComponent + serverIssuerUri.getPath();
		newUrl = stripTrailingSlash(newUrl);
		return newUrl;
	}

	public static String stripTrailingSlash(String path) {
		if (path != null && path.endsWith("/")) {
			return path.substring(0, path.length() - 1);
		}
		return path;
	}
}
