package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectSecondLoginPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "expect_second_login_page")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("The server must ask the user to login for a second time; a screenshot of this must be uploaded.");
		env.putString("expect_second_login_page", placeholder);

		return env;
	}
}
