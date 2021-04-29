package net.openid.conformance.condition.as.par;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureRequestObjectContainsCodeChallengeWhenUsingPAR extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_request_object" })
	@PostEnvironment(strings = { "code_challenge", "code_challenge_method" })
	public Environment evaluate(Environment env) {
		String codeChallenge = env.getString("authorization_request_object", "claims.code_challenge");
		String codeChallengeMethod = env.getString("authorization_request_object", "claims.code_challenge_method");

		if (Strings.isNullOrEmpty(codeChallenge)) {
			throw error("Missing required code_challenge parameter. PKCE is required when using PAR.");
		}
		if (Strings.isNullOrEmpty(codeChallengeMethod)) {
			throw error("Missing required code_challenge_method parameter. PKCE is required when using PAR.");
		}
		if (!"S256".equals(codeChallengeMethod)) {
			throw error("S256 is required for PKCE.", args("code_challenge_method", codeChallengeMethod));
		}
		env.putString("code_challenge", codeChallenge);
		env.putString("code_challenge_method", codeChallengeMethod);

		logSuccess("Found required PKCE parameters in request",
			args("code_challenge_method", codeChallengeMethod, "code_challenge", codeChallenge));
		return env;
	}

}
