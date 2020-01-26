package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectLoginPageWithPolicyLink extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "login_page_placeholder")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("The login page should show a link to a policy document - upload a screenshot of the login page.");
		env.putString("login_page_placeholder", placeholder);

		return env;
	}

}
