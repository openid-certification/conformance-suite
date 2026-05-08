package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.regex.Pattern;

/**
 * Validates the {@code OAuth-Client-Attestation-Challenge} response header on the most recent endpoint response
 * (mapped via {@code endpoint_response}) against the conformance suite's random-string conventions:
 * single instance, base64url charset (with the additional {@code .} and {@code ~} characters permitted by the
 * suite for random values), and a sane length range.
 *
 * Throws on findings; the caller decides severity (typically {@code WARNING}).
 */
public class ValidateClientAttestationChallengeResponseHeader extends AbstractCondition {

	private static final Pattern ALLOWED_CHARS = Pattern.compile("^[A-Za-z0-9_\\-.~]+$");
	private static final int MIN_LENGTH = 16;
	private static final int MAX_LENGTH = 512;

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement el = env.getElementFromObject("endpoint_response", "headers.oauth-client-attestation-challenge");
		if (el == null) {
			logSuccess("No OAuth-Client-Attestation-Challenge response header to validate");
			return env;
		}

		if (el.isJsonArray()) {
			throw error("Multiple OAuth-Client-Attestation-Challenge response headers received; the server should send at most one",
				args("OAuth-Client-Attestation-Challenge", el));
		}

		String challenge = OIDFJSON.getString(el);
		if (challenge.isEmpty()) {
			throw error("OAuth-Client-Attestation-Challenge response header is empty");
		}

		if (!ALLOWED_CHARS.matcher(challenge).matches()) {
			throw error("OAuth-Client-Attestation-Challenge response header contains characters outside the recommended set (base64url plus '.' and '~')",
				args("OAuth-Client-Attestation-Challenge", challenge));
		}

		if (challenge.length() < MIN_LENGTH) {
			throw error("OAuth-Client-Attestation-Challenge response header is shorter than expected; challenges should carry enough entropy to be unguessable",
				args("OAuth-Client-Attestation-Challenge", challenge, "length", challenge.length(), "minimum", MIN_LENGTH));
		}

		if (challenge.length() > MAX_LENGTH) {
			throw error("OAuth-Client-Attestation-Challenge response header is longer than expected",
				args("OAuth-Client-Attestation-Challenge", challenge, "length", challenge.length(), "maximum", MAX_LENGTH));
		}

		logSuccess("OAuth-Client-Attestation-Challenge response header is well-formed",
			args("OAuth-Client-Attestation-Challenge", challenge));
		return env;
	}
}
