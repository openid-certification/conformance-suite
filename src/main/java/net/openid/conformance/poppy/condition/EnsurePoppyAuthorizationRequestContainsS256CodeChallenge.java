package net.openid.conformance.poppy.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.regex.Pattern;

public class EnsurePoppyAuthorizationRequestContainsS256CodeChallenge extends AbstractCondition {

	private static final String REQUEST = "effective_authorization_endpoint_request";
	private static final Pattern S256_CHALLENGE = Pattern.compile("[A-Za-z0-9_-]{43}");

	@Override
	@PreEnvironment(required = REQUEST)
	@PostEnvironment(strings = {"code_challenge", "code_challenge_method"})
	public Environment evaluate(Environment env) {
		String challenge = env.getString(REQUEST, "code_challenge");
		String method = env.getString(REQUEST, "code_challenge_method");

		if (challenge == null || challenge.isEmpty()) {
			throw error("The authorization request has no 'code_challenge'. Direct Sign-In requires PKCE with S256 (section 4.2).");
		}
		if (method == null) {
			throw error("The authorization request has no 'code_challenge_method'. Without it the method defaults to 'plain' (RFC 7636 section 4.3), but Direct Sign-In requires S256 (section 4.2).");
		}
		if (!"S256".equals(method)) {
			throw error("The authorization request uses code_challenge_method '" + method + "' but Direct Sign-In requires S256 (section 4.2).",
				args("code_challenge_method", method));
		}
		if (!S256_CHALLENGE.matcher(challenge).matches()) {
			throw error("The code_challenge is not a base64url-encoded SHA-256 hash (43 characters from A-Z a-z 0-9 - _) as RFC 7636 section 4.2 defines for S256.",
				args("code_challenge", challenge));
		}

		env.putString("code_challenge", challenge);
		env.putString("code_challenge_method", method);
		logSuccess("The authorization request contains an S256 code_challenge",
			args("code_challenge", challenge, "code_challenge_method", method));
		return env;
	}
}
