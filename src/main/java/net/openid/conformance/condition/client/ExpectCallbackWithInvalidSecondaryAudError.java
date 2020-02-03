package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ExpectCallbackWithInvalidSecondaryAudError extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		createBrowserInteractionPlaceholder("The client must show an error message that there are multiple aud values in the id_token from the authorization_endpoint, and this behaviour is not expected - upload a log file or screenshot of the error.", true);
		return env;
	}

}
