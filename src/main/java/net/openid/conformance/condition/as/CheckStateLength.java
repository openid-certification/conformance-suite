package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckStateLength extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {

		String state = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE);

		if (! Strings.isNullOrEmpty(state)) {

			if (state.length() > 128) {
				throw error("State contains in excess of 128 characters. This may introduce interoperability issues.");
			}
		}

		logSuccess("State is empty or does not exceed 128 characters");
		return env;
	}
}
