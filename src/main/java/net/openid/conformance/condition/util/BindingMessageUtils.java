package net.openid.conformance.condition.util;

import java.util.regex.Pattern;

public final class BindingMessageUtils {

	private static final Pattern URL_PATTERN = Pattern.compile(
		"(?i)(?:\\b(?:https?|ftp)://\\S+|\\bmailto:\\S+|\\bwww\\.[a-z0-9][a-z0-9.-]*\\.[a-z]{2,}(?:/\\S*)?|"
			+ "\\b[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+/\\S*)");

	private BindingMessageUtils() {
	}

	public static boolean containsUrl(String value) {
		return value != null && URL_PATTERN.matcher(value).find();
	}
}
