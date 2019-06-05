package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExpectPKCEError extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "pkce_error")
	public Environment evaluate(Environment env) {
		String placeholder = createBrowserInteractionPlaceholder("Page showing PKCE is required");
		env.putString("pkce_error", placeholder);
		return env;

	}

}
