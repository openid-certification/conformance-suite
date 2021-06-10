package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ValidateCodeVerifierWithS256 extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "code_challenge", "code_challenge_method" }, required = "token_endpoint_request")
	public Environment evaluate(Environment env) {

		String codeVerifier = env.getString("token_endpoint_request", "body_form_params.code_verifier");

		if (Strings.isNullOrEmpty(codeVerifier)) {
			throw error("Couldn't find code_verifier in token request");
		}

		String codeChallenge = env.getString("code_challenge");
		String codeChallengeMethod = env.getString("code_challenge_method");
		if(!"S256".equals(codeChallengeMethod)) {
			throw error("Unexpected code_challenge_method", args("code_challenge_method", codeChallengeMethod));
		}

		try {
			byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(bytes, 0, bytes.length);
			byte[] digest = md.digest();
			String calculatedChallenge = Base64.encodeBase64URLSafeString(digest);
			if(codeChallenge.equals(calculatedChallenge)) {
				logSuccess("Validated code_verifier successfully",
					args("code_verifier", codeVerifier, "code_challenge", codeChallenge, "code_challenge_method", codeChallengeMethod));
				return env;
			} else {
				throw error("PKCE validation failed",
					args("expected_code_challenge", calculatedChallenge, "code_verifier", codeVerifier, "code_challenge", codeChallenge, "code_challenge_method", codeChallengeMethod));
			}

		} catch (NoSuchAlgorithmException e) {
			throw error("No such Algorithm Error",e);
		}
	}

}
