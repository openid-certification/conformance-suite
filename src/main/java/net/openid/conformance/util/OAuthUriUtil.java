package net.openid.conformance.util;

import java.net.URI;

public class OAuthUriUtil {

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
