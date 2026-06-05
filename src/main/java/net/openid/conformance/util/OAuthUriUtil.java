package net.openid.conformance.util;

import java.net.URI;

public class OAuthUriUtil {

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
