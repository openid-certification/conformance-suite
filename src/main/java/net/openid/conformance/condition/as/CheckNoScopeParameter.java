package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckNoScopeParameter extends AbstractEnsureResponseType {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {
		String scope = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "scope");

		if (scope != null) {
			throw error("The conformance tests require the use of DCQL passed in dcql_query. The scope parameter MUST NOT be used when a dcql_query parameter is present.");
		}

		logSuccess("scope parameter is not present, as expected.");

		return env;
	}

}
