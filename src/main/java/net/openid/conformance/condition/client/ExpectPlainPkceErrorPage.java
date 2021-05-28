package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectPlainPkceErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "plain_pkce_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Show an error page saying plain PKCE must not be used.");
		env.putString("plain_pkce_error", placeholder);

		return env;
	}

}
