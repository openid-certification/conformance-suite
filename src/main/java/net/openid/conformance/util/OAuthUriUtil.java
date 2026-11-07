package net.openid.conformance.util;

import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		if (!parameters.isEmpty()) {
			sb.append(beforeFragment.indexOf('?') == -1 ? '?' : '&');
			sb.append(formUrlEncodedQuery(parameters));
		}
		sb.append(fragment);
		return sb.toString();
	}

	/**
	 * The parameters as an application/x-www-form-urlencoded string without a leading '?', encoded as in
	 * {@link #appendFormUrlEncodedQuery}; for use as the fragment of a response_mode=fragment redirect.
	 */
	public static String formUrlEncodedQuery(Map<String, String> parameters) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(formUrlEncode(entry.getKey()));
			if (entry.getValue() != null) {
				sb.append('=');
				sb.append(formUrlEncode(entry.getValue()));
			}
		}
		return sb.toString();
	}

	private static String formUrlEncode(String value) {
		return UriUtils.encode(value, StandardCharsets.UTF_8);
	}

	/** Decodes a form-urlencoded name; a malformed percent sequence leaves it as sent, this is a diagnostic. */
	private static String decodeLeniently(String raw) {
		try {
			return java.net.URLDecoder.decode(raw, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			return raw;
		}
	}

	/**
	 * Parameters whose values are space-delimited lists or free text, so a literal '+' in them is the usual
	 * encoding of a space rather than a sign of a mis-encoded '+'.
	 */
	private static final Set<String> SPACE_CONTAINING_PARAMETERS = Set.of(
		"scope", "response_type", "prompt", "acr_values", "ui_locales", "claims_locales", "error_description");

	/**
	 * Names of the parameters in a raw application/x-www-form-urlencoded string (a URL query or a form body)
	 * whose name or value contains a literal '+'. A form decoder reads that as a space (RFC 6749 appendix B);
	 * space-delimited list and free-text parameters, where that is the expected encoding, are left out. Names are returned
	 * decoded, in order of first appearance, without duplicates.
	 */
	public static List<String> parametersWithLiteralPlus(String rawFormEncoded) {
		List<String> names = new ArrayList<>();
		if (rawFormEncoded == null || rawFormEncoded.indexOf('+') == -1) {
			return names;
		}
		for (String pair : rawFormEncoded.split("&")) {
			if (pair.indexOf('+') == -1) {
				continue;
			}
			int eq = pair.indexOf('=');
			String rawName = eq == -1 ? pair : pair.substring(0, eq);
			String name = decodeLeniently(rawName);
			if (SPACE_CONTAINING_PARAMETERS.contains(name)) {
				continue;
			}
			if (!names.contains(name)) {
				names.add(name);
			}
		}
		return names;
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
