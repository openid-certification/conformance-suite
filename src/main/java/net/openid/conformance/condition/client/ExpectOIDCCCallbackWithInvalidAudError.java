package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ExpectOIDCCCallbackWithInvalidAudError extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		createBrowserInteractionPlaceholder("The client must show an error message that the 'aud' value is incorrect - upload a log file or screenshot of the error.");
		return env;
	}

}
