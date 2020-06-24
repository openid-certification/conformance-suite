package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectSuccessfulLogoutPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "successful_logout_page")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("The server must log the user out - upload a screenshot of the successful logout page.");
		env.putString("successful_logout_page", placeholder);

		return env;
	}
}
