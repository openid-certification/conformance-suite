package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectRedirectUriMissingErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "redirect_uri_missing_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Show redirect URI error page");
		env.putString("redirect_uri_missing_error", placeholder);

		return env;
	}

}
