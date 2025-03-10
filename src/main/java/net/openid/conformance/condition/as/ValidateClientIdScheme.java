package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientIdScheme extends AbstractEnsureResponseType {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {
		String expectedScheme = env.getString("client_id_scheme");
		String clientIdScheme = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_id_scheme");

		if (clientIdScheme == null) {
			throw error("client_id_scheme is missing from request");
		}

		if (!clientIdScheme.equals(expectedScheme)) {
			throw error("client_id_scheme must be '" + expectedScheme + "'", args("actual", clientIdScheme));
		}

		logSuccess("client_id_scheme is " + expectedScheme);
		return env;
	}

}
