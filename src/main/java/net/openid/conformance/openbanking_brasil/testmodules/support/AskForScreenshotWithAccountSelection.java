package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AskForScreenshotWithAccountSelection extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "redirect_uri_missing_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Upload a screenshot showing the user is presented with an opion to select an account");
		env.putString("redirect_uri_missing_error", placeholder);

		return env;
	}

}
