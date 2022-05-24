package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectIdTokenHintRequiredErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "id_token_hint_required_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("The server must show an error page saying the request is invalid as the id_token_hint is missing - upload a screenshot of the error page.");
		env.putString("id_token_hint_required_error", placeholder);

		return env;
	}
}
