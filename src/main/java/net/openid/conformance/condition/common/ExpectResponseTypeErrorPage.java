package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectResponseTypeErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "response_type_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("If the server does not return an error back to the client, " +
				"upload a screenshot of the error page showing an invalid response type error.");
		env.putString("response_type_error", placeholder);

		return env;
	}

}
