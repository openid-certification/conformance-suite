package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateResponseMode extends AbstractEnsureResponseType {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {
		String expectedMode = env.getString("response_mode");
		String responseModeString = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "response_mode");

		if (responseModeString == null) {
			throw error("response_mode is missed from request");
		}

		if (!responseModeString.equals(expectedMode)) {
			throw error("response_mode must be '" + expectedMode + "'", args("actual", responseModeString));
		}

		logSuccess("response_mode is " + expectedMode);
		return env;
	}

}
