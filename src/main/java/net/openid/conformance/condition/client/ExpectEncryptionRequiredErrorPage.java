package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectEncryptionRequiredErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "encryption_required_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Show an error page saying the request object must be encrypted when passed via the browser.");
		env.putString("encryption_required_error", placeholder);

		return env;
	}

}
