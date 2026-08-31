package net.openid.conformance.condition.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BindingMessageUtils {

	/*
	 * Detect unambiguous URL-like values only. Bare non-www domains and bare IP literals are
	 * deliberately not matched to avoid false positives in a binding_message. Explicitly recognized
	 * schemes are http, https, ftp, and mailto; the host/path alternative is scheme-agnostic.
	 */
	private static final Pattern URL_PATTERN = Pattern.compile(
		"(?i)(?<absoluteUrl>\\b(?:https?|ftp)://\\S+)|(?<mailto>\\bmailto:\\S+)|"
			+ "(?<www>\\bwww\\.[a-z0-9][a-z0-9.-]*\\.[a-z]{2,}(?:/\\S*)?)|"
			+ "(?<hostPath>\\b[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"
			+ "(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)*\\.[a-z]{2,}/\\S*)");

	private BindingMessageUtils() {
	}

	public static boolean containsUrl(String value) {
		return findUrl(value).isPresent();
	}

	public static Optional<UrlMatch> findUrl(String value) {
		if (value == null) {
			return Optional.empty();
		}

		Matcher matcher = URL_PATTERN.matcher(value);
		if (!matcher.find()) {
			return Optional.empty();
		}

		String type;
		String sanitizedValue;
		if (matcher.group("absoluteUrl") != null) {
			type = "absolute_url";
			sanitizedValue = sanitizeAbsoluteUrl(matcher.group());
		} else if (matcher.group("mailto") != null) {
			type = "mailto";
			sanitizedValue = "mailto:[redacted]";
		} else if (matcher.group("www") != null) {
			type = "www";
			sanitizedValue = sanitizeHostPath(matcher.group());
		} else {
			type = "host_path";
			sanitizedValue = sanitizeHostPath(matcher.group());
		}

		return Optional.of(new UrlMatch(type, matcher.start(), matcher.end() - matcher.start(), sanitizedValue));
	}

	private static String sanitizeAbsoluteUrl(String url) {
		int authorityStart = url.indexOf("://") + 3;
		int authorityEnd = firstIndexOfAny(url, authorityStart, '/', '?', '#');
		if (authorityEnd < 0) {
			authorityEnd = url.length();
		}

		String authority = url.substring(authorityStart, authorityEnd);
		int userInfoEnd = authority.lastIndexOf('@');
		if (userInfoEnd >= 0) {
			authority = authority.substring(userInfoEnd + 1);
		}

		String scheme = url.substring(0, authorityStart);
		String suffix = authorityEnd < url.length() ? "/[redacted]" : "";
		return scheme + authority + suffix;
	}

	private static String sanitizeHostPath(String url) {
		int pathStart = url.indexOf('/');
		return pathStart < 0 ? url : url.substring(0, pathStart) + "/[redacted]";
	}

	private static int firstIndexOfAny(String value, int start, char... candidates) {
		for (int index = start; index < value.length(); index++) {
			char current = value.charAt(index);
			for (char candidate : candidates) {
				if (current == candidate) {
					return index;
				}
			}
		}
		return -1;
	}

	public record UrlMatch(String type, int start, int length, String sanitizedValue) {
	}
}
