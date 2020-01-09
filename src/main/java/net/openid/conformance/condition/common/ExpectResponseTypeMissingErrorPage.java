package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectResponseTypeMissingErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "response_type_missing_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Upload a screenshot of the error page showing a missing response type error.");
		env.putString("response_type_missing_error", placeholder);

		return env;
	}

}
