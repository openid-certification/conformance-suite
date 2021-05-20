package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectPkceMissingErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "pkce_missing_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Show an error page saying PKCE / code_challenge / code_challenge_method are missing from the request.");
		env.putString("pkce_missing_error", placeholder);

		return env;
	}

}
