package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractEnsureMinimumEntropy;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMinimumPkceCodeVerifierEntropy extends AbstractEnsureMinimumEntropy {
	/**
	 * https://www.rfc-editor.org/rfc/rfc7636.html#section-7.1
	 *
	 *    The client SHOULD create a "code_verifier" with a minimum of 256 bits
	 *    of entropy.  This can be done by having a suitable random number
	 *    generator create a 32-octet sequence.  The octet sequence can then be
	 *    base64url-encoded to produce a 43-octet URL safe string to use as a
	 *    "code_challenge" that has the required entropy.
	 *
	 * The actual amount of required entropy is 256 bits, but we can't accurately measure entropy so a bit of
	 * slop is allowed for.
	 */
	private final double requiredEntropy = 180;

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

		return ensureMinimumEntropy(env, codeVerifier, requiredEntropy);
	}

}
