package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectInvalidAudienceErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "invalid_aud_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Show an error page saying the 'aud' in the request object is invalid.");
		env.putString("invalid_aud_error", placeholder);

		return env;
	}

}
