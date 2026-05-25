package net.openid.conformance.condition.util;

/**
 * Validates a single OAuth 2.0 scope-token against the RFC 6749 Appendix A.4 ABNF:
 * <pre>
 *   scope-token = 1*( %x21 / %x23-5B / %x5D-7E )
 *   scope       = scope-token *( SP scope-token )
 * </pre>
 * i.e., one or more visible ASCII characters excluding SP (0x20), DQUOTE (0x22),
 * and BACKSLASH (0x5C). This class checks a single scope-token — callers that
 * accept space-separated lists must split on SP first.
 */
public final class ScopeTokenSyntaxUtil {

	private ScopeTokenSyntaxUtil() {
	}

	public static boolean isValidScopeToken(String token) {
		return scopeTokenSyntaxError(token) == null;
	}

	/**
	 * Returns a human-readable explanation of why {@code token} is not a valid RFC 6749
	 * Appendix A.4 scope-token (naming the offending character and its position), or
	 * {@code null} if {@code token} is a valid scope-token.
	 */
	public static String scopeTokenSyntaxError(String token) {
		if (token == null || token.isEmpty()) {
			return "is empty; an RFC 6749 Appendix A.4 scope-token must contain at least one character";
		}
		for (int i = 0; i < token.length(); i++) {
			char c = token.charAt(i);
			if (!isValidScopeTokenChar(c)) {
				return String.format("contains %s at index %d, which is not permitted by the RFC 6749 Appendix A.4 "
						+ "scope-token ABNF (allowed characters are %%x21, %%x23-5B and %%x5D-7E: visible ASCII "
						+ "except space, double-quote and backslash)",
					describeChar(c), i);
			}
		}
		return null;
	}

	private static boolean isValidScopeTokenChar(char c) {
		return c == 0x21 || (c >= 0x23 && c <= 0x5B) || (c >= 0x5D && c <= 0x7E);
	}

	private static String describeChar(char c) {
		return switch (c) {
			case ' ' -> "' ' (0x20 SPACE)";
			case '"' -> "'\"' (0x22 DQUOTE)";
			case '\\' -> "'\\' (0x5C BACKSLASH)";
			default -> {
				if (c < 0x20 || c == 0x7F) {
					yield String.format("0x%02X (control character)", (int) c);
				}
				if (c > 0x7E) {
					yield String.format("U+%04X (non-ASCII character)", (int) c);
				}
				yield String.format("'%c' (0x%02X)", c, (int) c);
			}
		};
	}
}
