package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractEnsureResponseType;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.testmodule.Environment;

public class EnsureBackchannelResponseTypeIsCodeIdToken extends AbstractEnsureResponseType {

	@Override
	@PreEnvironment(required = CreateEffectiveBackchannelRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {
		return ensureResponseTypeMatches(env, "code", "id_token");
	}

}
