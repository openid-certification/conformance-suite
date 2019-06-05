package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExpectRejectPlainCodeChallengeMethodErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "plain_pkce_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Show plain code challenge method error page");
		env.putString("plain_pkce_error", placeholder);

		return env;
	}

}
