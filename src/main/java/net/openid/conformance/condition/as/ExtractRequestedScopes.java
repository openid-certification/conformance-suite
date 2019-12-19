package net.openid.conformance.condition.as;

import com.google.common.base.Strings;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractRequestedScopes extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {

		String scope = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.SCOPE);

		if (Strings.isNullOrEmpty(scope)) {
			throw error("Missing scope parameter");
		} else {
			logSuccess("Requested scopes", args("scope", scope));

			env.putString("scope", scope);

			return env;
		}

	}

}
