package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectIdTokenHintRequiredErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "id_token_hint_required_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("The server must show an error page saying the request is invalid as the post_logout_redirect_uri is not a registered one - upload a screenshot of the error page.");
		env.putString("id_token_hint_required_error", placeholder);

		return env;
	}
}
