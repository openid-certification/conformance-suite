package net.openid.conformance.openid.federation;

import java.util.Objects;

public class EntityUtils {

	public static String appendWellKnown(String entityIdentifier) {
		if (entityIdentifier.endsWith(".well-known/openid-federation")) {
			return entityIdentifier;
		}
		if (entityIdentifier.endsWith("/")) {
			return entityIdentifier + ".well-known/openid-federation";
		}
		return entityIdentifier + "/.well-known/openid-federation";
	}

	public static String stripWellKnown(String url) {
		String entityIdentifier = url;
		final String removingPartInUrl = ".well-known/openid-federation";
		if (url.endsWith(removingPartInUrl)) {
			entityIdentifier = url.substring(0, url.length() - removingPartInUrl.length());
		}
		return entityIdentifier;
	}

	public static String stripTrailingSlash(String url) {
		String entityIdentifier = url;
		final String removingPartInUrl = "/";
		if (url.endsWith(removingPartInUrl)) {
			entityIdentifier = url.substring(0, url.length() - removingPartInUrl.length());
		}
		return entityIdentifier;
	}

	public static boolean equals(String a, String b) {
		return Objects.equals(a, b) || Objects.equals(stripTrailingSlash(a), stripTrailingSlash(b));
	}
}
