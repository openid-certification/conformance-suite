package net.openid.conformance.openid.ssf.conditions;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Asserts that the Bearer {@code WWW-Authenticate} challenge of a rejected resource request
 * carries the expected RFC 6750 section 3.1 error code ({@code invalid_token} for a 401 with a
 * bad token, {@code insufficient_scope} for a 403). The CAEP Interop Profile (2.7.2) requires the
 * transmitter to return errors as per RFC 6750 section 3.1 when the access token is not
 * sufficient for the requested action.
 * <p>
 * Not applicable to requests that carry no authentication information at all: for those RFC 6750
 * section 3.1 says the resource server SHOULD NOT include an error code.
 * <p>
 * Expects the response under {@code endpoint_response} (map {@code resource_endpoint_response_full}
 * onto it before calling).
 */
public class OIDSSFEnsureWwwAuthenticateErrorCode extends AbstractCondition {

	/**
	 * One {@code name=value} auth-param of a challenge; the value is either a quoted-string
	 * (group 2, backslash escapes allowed) or a token (group 3).
	 */
	private static final Pattern AUTH_PARAM = Pattern.compile("([A-Za-z0-9!#$%&'*+.^_`|~-]+)\\s*=\\s*(?:\"((?:[^\"\\\\]|\\\\.)*)\"|([^\\s,\"]+))");

	private final String expectedErrorCode;

	public OIDSSFEnsureWwwAuthenticateErrorCode(String expectedErrorCode) {
		this.expectedErrorCode = expectedErrorCode;
	}

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement headersEl = env.getElementFromObject("endpoint_response", "headers");
		if (headersEl == null || !headersEl.isJsonObject()) {
			throw error("Could not find the response headers of the rejected request",
				args("endpoint_response", env.getObject("endpoint_response"), "expected_error", expectedErrorCode));
		}

		String challenge = OIDSSFEnsureWwwAuthenticateHeaderPresent.findWwwAuthenticateHeader(headersEl.getAsJsonObject());
		if (challenge == null || challenge.isBlank()) {
			throw error("The rejected request did not carry a 'WWW-Authenticate' response header, so it cannot carry the "
					+ "bearer error code a resource server must return for a rejected request",
				args("response_headers", headersEl, "expected_error", expectedErrorCode));
		}

		if (!challenge.regionMatches(true, 0, "Bearer", 0, "Bearer".length())) {
			throw error("The 'WWW-Authenticate' response header does not contain a 'Bearer' challenge",
				args("www_authenticate", challenge, "expected_error", expectedErrorCode));
		}

		Map<String, String> authParams = parseAuthParams(challenge.substring("Bearer".length()));
		String error = authParams.get("error");

		if (error == null) {
			throw error("The Bearer 'WWW-Authenticate' challenge does not carry an 'error' parameter. "
					+ "A resource server rejecting a request that presented credentials must name the bearer error code.",
				args("www_authenticate", challenge, "auth_params", authParams, "expected_error", expectedErrorCode));
		}

		if (!expectedErrorCode.equals(error)) {
			throw error("The Bearer 'WWW-Authenticate' challenge carries an unexpected bearer error code",
				args("www_authenticate", challenge, "error", error, "expected_error", expectedErrorCode));
		}

		logSuccess("The Bearer 'WWW-Authenticate' challenge carries the expected bearer error code",
			args("www_authenticate", challenge, "error", error));

		return env;
	}

	/**
	 * Parses the auth-params of a Bearer challenge (RFC 6750 section 3, RFC 9110 section 11.2)
	 * into a map keyed by lower-cased parameter name. Values may be quoted or bare, in any order.
	 */
	static Map<String, String> parseAuthParams(String authParams) {
		Map<String, String> params = new LinkedHashMap<>();
		Matcher matcher = AUTH_PARAM.matcher(authParams);
		while (matcher.find()) {
			String name = matcher.group(1).toLowerCase(Locale.ROOT);
			String value = matcher.group(2) != null
				? matcher.group(2).replaceAll("\\\\(.)", "$1")
				: matcher.group(3);
			params.putIfAbsent(name, value);
		}
		return params;
	}
}
