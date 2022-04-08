package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMinimumPkceCodeVerifierLength extends AbstractCondition {

	/**
	 * 	  https://www.rfc-editor.org/rfc/rfc7636.html#section-7.3
	 *
	 *    code_verifier = high-entropy cryptographic random STRING using the
	 *    unreserved characters [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"
	 *    from Section 2.3 of [RFC3986], with a minimum length of 43 characters
	 *    and a maximum length of 128 characters.
	 *
	 *    NOTE: The code verifier SHOULD have enough entropy to make it
	 *    impractical to guess the value.  It is RECOMMENDED that the output of
	 *    a suitable random number generator be used to create a 32-octet
	 *    sequence.  The octet sequence is then base64url-encoded to produce a
	 *    43-octet URL safe string to use as the code verifier.
	 */
	private final int requiredLength = 344;  // 43 * 8

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	public Environment evaluate(Environment env) {

		String codeVerifier = env.getString("token_endpoint_request", "body_form_params.code_verifier");

		if (Strings.isNullOrEmpty(codeVerifier)) {
			throw error("Couldn't find code_verifier in token request");
		}

		if (Strings.isNullOrEmpty(codeVerifier)) {
			throw error("Can't find access token");
		}

		byte[] bytes = codeVerifier.getBytes();

		int bitLength = bytes.length * 8;

		if (bitLength >= requiredLength) {
			logSuccess("PKCE code verifier is of sufficient length", args("required", requiredLength, "actual", bitLength));
			return env;
		} else {
			throw error("PKCE code verifier is not of sufficient length", args("required", requiredLength, "actual", bitLength));
		}

	}

}
