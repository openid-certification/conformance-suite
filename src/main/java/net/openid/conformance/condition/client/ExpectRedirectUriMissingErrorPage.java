package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectRedirectUriMissingErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "redirect_uri_missing_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Show an error page saying the redirect uri is missing from the request.");
		env.putString("redirect_uri_missing_error", placeholder);

		return env;
	}

}
