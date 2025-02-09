package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckNoClientIdSchemeParameter extends AbstractEnsureResponseType {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {
		String clientIdScheme = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_id_scheme");

		if (clientIdScheme != null) {
			throw error("The client_id_scheme parameter was removed in ID3 of OpenID4VP and must not be used.");
		}

		logSuccess("client_id_scheme parameter is not present, as expected.");

		return env;
	}

}
