package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureResponseTypeIsVpToken extends AbstractEnsureResponseType {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {

		return ensureResponseTypeMatches(env, "vp_token");

	}

}
