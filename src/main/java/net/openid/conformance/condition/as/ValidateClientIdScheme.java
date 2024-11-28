package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientIdScheme extends AbstractEnsureResponseType {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {
		String expectedMode = env.getString("client_id_scheme");
		String responseModeString = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_id_scheme");

		if (responseModeString == null) {
			throw error("client_id_scheme is missed from request");
		}

		if (!responseModeString.equals(expectedMode)) {
			throw error("client_id_scheme must be '" + expectedMode + "'", args("actual", responseModeString));
		}

		logSuccess("client_id_scheme is " + expectedMode);
		return env;
	}

}
