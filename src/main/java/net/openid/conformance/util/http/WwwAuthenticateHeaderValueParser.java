package net.openid.conformance.util.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WwwAuthenticateHeaderValueParser {

	private static final Pattern PARAM_PATTERN = Pattern.compile(
		"([a-zA-Z][a-zA-Z0-9_-]*)\\s*=\\s*\"((?:\\\\.|[^\"\\\\])*)\"|([a-zA-Z][a-zA-Z0-9_-]*)\\s*=\\s*([^\",\\s]+)");

	/**
	 * Returns the challenges from the WWW-Authenticate header value. The challenge type is the key, the attributes are stored as the kv-map value.
	 * @param headerValue
	 * @return
	 */
	public static Map<String, Map<String, String>> parse(String headerValue) {
		Map<String, Map<String, String>> result = new LinkedHashMap<>();
		List<String> parts = splitChallenges(headerValue);

		for (String part : parts) {
			String[] schemeSplit = part.trim().split("\\s+", 2);
			String scheme = schemeSplit[0];
			String paramString = schemeSplit.length > 1 ? schemeSplit[1] : "";
			Map<String, String> params = new LinkedHashMap<>();

			if (!paramString.isEmpty()) {
				Matcher m = PARAM_PATTERN.matcher(paramString);
				int lastMatchEnd = 0;
				while (m.find()) {
					if (m.group(1) != null) {
						params.put(m.group(1), unescapeQuoted(m.group(2)));
					} else {
						params.put(m.group(3), m.group(4));
					}
					lastMatchEnd = m.end();
				}

				// see: https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/WWW-Authenticate
				// Check for trailing token without "=" (e.g., token68)
				if (lastMatchEnd < paramString.length()) {
					String trailing = paramString.substring(lastMatchEnd).trim();
					if (trailing.startsWith(",")) {
						trailing = trailing.substring(1).trim();
					}
					if (!trailing.isEmpty() && trailing.matches("^[a-zA-Z][a-zA-Z0-9_-]*$")) {
						params.put(trailing, null);
					}
				}
			}

			result.put(scheme, params);
		}

		return result;
	}

	/**
	 * Extracts the found challenges as a Map with the challenge type as key, and the original challenge as value.
	 * @param headerValue
	 * @return
	 */
	public static Map<String, String> extractChallenges(String headerValue) {

		if (headerValue == null || headerValue.isBlank()) {
			return Collections.emptyMap();
		}

		Map<String, String> result = new LinkedHashMap<>();
		List<String> parts = splitChallenges(headerValue);

		for (String part : parts) {
			String trimmed = part.trim();
			int space = trimmed.indexOf(' ');
			String scheme = space > 0 ? trimmed.substring(0, space) : trimmed;
			result.put(scheme, trimmed);
		}

		return result;
	}

	private static List<String> splitChallenges(String header) {
		List<String> result = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < header.length(); i++) {
			char c = header.charAt(i);
			if (c == '"') {
				inQuotes = !inQuotes || (i > 0 && header.charAt(i - 1) == '\\');
			} else if (c == ',' && !inQuotes) {
				// Look ahead for scheme start
				int j = i + 1;
				while (j < header.length() && Character.isWhitespace(header.charAt(j))){
					j++;
				}
				int k = j;
				while (k < header.length() && Character.isLetter(header.charAt(k))){
					k++;
				}
				if (k < header.length() && header.charAt(k) == ' ') {
					result.add(current.toString());
					current.setLength(0);
					continue;
				}
			}
			current.append(c);
		}
		if (!current.isEmpty()){
			result.add(current.toString());
		}
		return result;
	}

	private static String unescapeQuoted(String input) {
		return input.replace("\\\"", "\"").replace("\\\\", "\\");
	}
}
