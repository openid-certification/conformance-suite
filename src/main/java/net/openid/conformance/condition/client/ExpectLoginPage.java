package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectLoginPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "login_page_placeholder")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("The login page should be shown, if not upload a screenshot of the error page.");
		env.putString("login_page_placeholder", placeholder);

		return env;
	}
}
