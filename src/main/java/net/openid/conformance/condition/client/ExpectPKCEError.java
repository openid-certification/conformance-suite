package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectPKCEError extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "pkce_error")
	public Environment evaluate(Environment env) {
		String placeholder = createBrowserInteractionPlaceholder("Page showing PKCE is required");
		env.putString("pkce_error", placeholder);
		return env;

	}

}
