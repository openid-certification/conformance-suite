package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMatchingClientId extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", CreateEffectiveAuthorizationRequestParameters.ENV_KEY})
	public Environment evaluate(Environment env) {

		// get the client ID from the configuration
		String expected = env.getString("client", "client_id");
		String actual = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.CLIENT_ID);

		if (!Strings.isNullOrEmpty(expected) && expected.equals(actual)) {
			logSuccess("Client ID matched",
				args("client_id", Strings.nullToEmpty(actual)));
			return env;
		} else {
			throw error("Mismatch between Client ID in test configuration and the one in the authorization request", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
		}

	}

}
