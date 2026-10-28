package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks that the Accept header of a credential issuer metadata request follows the RFC 9110
 * section 12.5.1 grammar: a comma-separated list of media ranges, each followed by
 * semicolon-separated name=value parameters. A wallet that separates media types with semicolons
 * ("application/jwt; application/json") sends a single media range with a malformed parameter, so
 * an issuer may not see the second type at all.
 *
 * <p>An absent Accept header is not flagged: OID4VCI 1.0 section 12.2.2 only recommends sending one.
 */
public class VCICheckCredentialIssuerMetadataRequestAcceptHeaderSyntax extends AbstractCondition {

	private static final String TOKEN = "[!#$%&'*+.^_`|~0-9A-Za-z-]+";
	private static final String QUOTED_STRING = "\"(?:[\\t \\x21\\x23-\\x5B\\x5D-\\x7E\\x80-\\xFF]|\\\\[\\t \\x21-\\x7E\\x80-\\xFF])*\"";
	private static final Pattern MEDIA_RANGE = Pattern.compile(
		TOKEN + "/" + TOKEN + "(?:[ \\t]*;[ \\t]*(?:" + TOKEN + "=(?:" + TOKEN + "|" + QUOTED_STRING + "))?)*");

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {
		String accept = env.getString("incoming_request", "headers.accept");
		if (accept == null) {
			logSuccess("The credential issuer metadata request has no Accept header, which OID4VCI only recommends sending");
			return env;
		}

		List<String> invalid = new ArrayList<>();
		for (String element : splitListElements(accept)) {
			String trimmed = element.strip();
			if (trimmed.isEmpty() || !MEDIA_RANGE.matcher(trimmed).matches()) {
				invalid.add(trimmed);
			}
		}

		if (!invalid.isEmpty()) {
			throw error("The Accept header of the credential issuer metadata request is not a valid list of media ranges."
					+ " Media ranges must be separated by commas; a semicolon introduces a name=value parameter of the preceding media range,"
					+ " and empty list elements must not be sent.",
				args("accept", accept, "invalid_elements", invalid));
		}

		logSuccess("The Accept header of the credential issuer metadata request is a valid list of media ranges", args("accept", accept));
		return env;
	}

	/** Splits an HTTP list on the commas that are not inside a quoted-string. */
	static List<String> splitListElements(String value) {
		List<String> elements = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		boolean escaped = false;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (escaped) {
				escaped = false;
			} else if (quoted && c == '\\') {
				escaped = true;
			} else if (c == '"') {
				quoted = !quoted;
			} else if (c == ',' && !quoted) {
				elements.add(current.toString());
				current.setLength(0);
				continue;
			}
			current.append(c);
		}
		elements.add(current.toString());
		return elements;
	}
}
