package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectInvalidIdTokenHintErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "invalid_id_token_hint_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("The server must show an error page saying the request is invalid as the id_token_hint is not valid - upload a screenshot of the error page.");
		env.putString("invalid_id_token_hint_error", placeholder);

		return env;
	}
}
