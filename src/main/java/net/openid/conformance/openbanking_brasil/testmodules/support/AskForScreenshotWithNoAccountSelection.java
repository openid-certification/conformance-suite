package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AskForScreenshotWithNoAccountSelection extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "payments_placeholder")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Upload a screenshot showing the user is not presented with an option to select an account at the bank");
		env.putString("payments_placeholder", placeholder);

		return env;
	}

}
