package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureAuthorizationRequestContainsPkceCodeChallenge extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY)
	@PostEnvironment(strings = { "code_challenge", "code_challenge_method" })
	public Environment evaluate(Environment env) {
		String codeChallenge = env.getString(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, CreateEffectiveAuthorizationPARRequestParameters.CODE_CHALLENGE);
		String codeChallengeMethod = env.getString(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, CreateEffectiveAuthorizationPARRequestParameters.CODE_CHALLENGE_METHOD);

		if (Strings.isNullOrEmpty(codeChallenge)) {
			throw error("Missing required code_challenge parameter.");
		}
		if (Strings.isNullOrEmpty(codeChallengeMethod)) {
			throw error("Missing required code_challenge_method parameter.");
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
