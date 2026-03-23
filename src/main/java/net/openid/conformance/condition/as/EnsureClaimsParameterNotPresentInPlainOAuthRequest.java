package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureClaimsParameterNotPresentInPlainOAuthRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {

		if (env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "claims") != null) {
			throw error("The 'claims' parameter is an OpenID Connect extension and should not be present in a plain OAuth2 authorization request. Change the client type in the test configuration to 'oidc' to test OpenID Connect.");
		}

		logSuccess("Authorization request does not contain the OpenID Connect 'claims' parameter");
		return env;
	}
}
