package net.openid.conformance.poppy.condition;

import java.net.URI;
import java.net.URISyntaxException;

final class PoppyUtil {

	private PoppyUtil() {
	}

	/**
	 * True if the value parses as an absolute URL with the https scheme and a host.
	 */
	static boolean isHttpsUrl(String value) {
		if (value == null) {
			return false;
		}
		try {
			URI uri = new URI(value);
			return "https".equals(uri.getScheme()) && uri.getHost() != null;
		} catch (URISyntaxException e) {
			return false;
		}
	}

	/**
	 * True if the value is exactly {@code https://} plus the host, with the host compared ignoring
	 * case and no port, path, query or trailing slash.
	 */
	static boolean isHttpsOriginOf(String value, String domain) {
		if (!isHttpsUrl(value)) {
			return false;
		}
		URI uri = URI.create(value);
		return uri.getPort() == -1
			&& domain.equalsIgnoreCase(uri.getHost())
			&& (uri.getRawPath() == null || uri.getRawPath().isEmpty())
			&& uri.getRawQuery() == null
			&& uri.getRawFragment() == null
			&& !value.endsWith("/");
	}
}
