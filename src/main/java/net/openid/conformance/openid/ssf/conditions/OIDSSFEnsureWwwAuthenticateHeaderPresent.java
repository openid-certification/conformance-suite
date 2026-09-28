package net.openid.conformance.openid.ssf.conditions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Asserts that a rejected resource request carries a Bearer {@code WWW-Authenticate} challenge
 * (RFC 6750 section 3). A bare {@code WWW-Authenticate: Bearer} without an error code is
 * accepted - for a request without any authentication information, RFC 6750 section 3.1 even
 * says the server SHOULD NOT include an error code. The header may be sent more than once and
 * each value may list several challenges (RFC 9110 section 11.6.1); the Bearer challenge is
 * accepted in any position.
 * <p>
 * The severity is the caller's: for the poll endpoint, RFC 8936 section 3 itself says a SET
 * delivery endpoint using HTTP authentication SHALL name its schemes in this header, a FAILURE.
 * For the stream management endpoints the requirement reaches the suite through the CAEP
 * Interop Profile (2.7.2), which cites RFC 6750 section 3.1 (the error codes) rather than
 * section 3 (the header that carries them), so those callers grade a missing header as a
 * WARNING; the rejection itself (401/403) remains their FAILURE-level check.
 * <p>
 * Expects the response under {@code endpoint_response} (map {@code resource_endpoint_response_full}
 * onto it before calling).
 */
public class OIDSSFEnsureWwwAuthenticateHeaderPresent extends AbstractCondition {

	/**
	 * A list element that starts a challenge: an auth-scheme token followed by whitespace or
	 * the end, as opposed to an auth-param, whose token is followed by {@code =}.
	 */
	private static final Pattern CHALLENGE_START = Pattern.compile("^[A-Za-z0-9!#$%&'*+.^_`|~-]+(?:\\s+(?!=).*)?$", Pattern.DOTALL);

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement headersEl = env.getElementFromObject("endpoint_response", "headers");
		if (headersEl == null || !headersEl.isJsonObject()) {
			throw error("Could not find the response headers of the rejected request",
				args("endpoint_response", env.getObject("endpoint_response")));
		}

		List<String> headerValues = findWwwAuthenticateHeaderValues(headersEl.getAsJsonObject());

		if (headerValues.stream().allMatch(String::isBlank)) {
			throw error("The rejected request did not carry a 'WWW-Authenticate' response header. "
					+ "A bearer-token resource server must include a 'WWW-Authenticate' challenge when rejecting a request; "
					+ "the bearer error codes are carried in that header.",
				args("response_headers", headersEl));
		}

		String challenge = findBearerChallenge(headerValues);
		if (challenge == null) {
			throw error("The 'WWW-Authenticate' response header does not contain a 'Bearer' challenge",
				args("www_authenticate", headerValues));
		}

		logSuccess("The rejected request carried a Bearer 'WWW-Authenticate' challenge",
			args("www_authenticate", headerValues, "bearer_challenge", challenge));

		return env;
	}

	/**
	 * The values of the {@code WWW-Authenticate} response header (name matched case-insensitively;
	 * several values if the header was sent more than once), or an empty list if it is absent.
	 */
	static List<String> findWwwAuthenticateHeaderValues(JsonObject headers) {
		for (Map.Entry<String, JsonElement> header : headers.entrySet()) {
			if ("www-authenticate".equalsIgnoreCase(header.getKey())) {
				List<String> values = new ArrayList<>();
				List<JsonElement> elements = header.getValue().isJsonArray()
					? header.getValue().getAsJsonArray().asList()
					: List.of(header.getValue());
				for (JsonElement element : elements) {
					String value = OIDFJSON.tryGetString(element);
					if (value != null) {
						values.add(value);
					}
				}
				return values;
			}
		}
		return List.of();
	}

	/**
	 * The Bearer challenge among the challenges the header values list, starting with the scheme
	 * and carrying only that challenge's auth-params, or {@code null} if there is none.
	 */
	static String findBearerChallenge(List<String> headerValues) {
		for (String headerValue : headerValues) {
			for (String challenge : splitChallenges(headerValue)) {
				String scheme = challenge.split("[\\s,]", 2)[0];
				if ("Bearer".equalsIgnoreCase(scheme)) {
					return challenge;
				}
			}
		}
		return null;
	}

	/**
	 * Splits a header value into its challenges. Both the challenges and the auth-params within
	 * one challenge are comma-separated (RFC 9110 section 11.6.1), so the value is cut at the
	 * commas outside quoted strings and an element that starts with a scheme token opens a new
	 * challenge while an auth-param continues the current one.
	 */
	static List<String> splitChallenges(String headerValue) {
		List<String> challenges = new ArrayList<>();
		StringBuilder current = null;
		for (String element : splitOutsideQuotedStrings(headerValue)) {
			String trimmed = element.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			if (current == null || CHALLENGE_START.matcher(trimmed).matches()) {
				if (current != null) {
					challenges.add(current.toString());
				}
				current = new StringBuilder(trimmed);
			} else {
				current.append(", ").append(trimmed);
			}
		}
		if (current != null) {
			challenges.add(current.toString());
		}
		return challenges;
	}

	private static List<String> splitOutsideQuotedStrings(String value) {
		List<String> elements = new ArrayList<>();
		StringBuilder element = new StringBuilder();
		boolean quoted = false;
		boolean escaped = false;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (escaped) {
				element.append(c);
				escaped = false;
				continue;
			}
			if (quoted && c == '\\') {
				escaped = true;
			} else if (c == '"') {
				quoted = !quoted;
			}
			if (c == ',' && !quoted) {
				elements.add(element.toString());
				element.setLength(0);
			} else {
				element.append(c);
			}
		}
		elements.add(element.toString());
		return elements;
	}
}
