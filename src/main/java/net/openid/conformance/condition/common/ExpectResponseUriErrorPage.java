package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectResponseUriErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "response_uri_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Show invalid response URI error page");
		env.putString("response_uri_error", placeholder);

		return env;
	}

}
