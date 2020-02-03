package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ExpectCallbackWithUnexpectedAlgorithmError extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		createBrowserInteractionPlaceholder("The client must show an error message that the algorithm used to sign the id_token does not match the required algorithm - upload a log file or screenshot of the error.", true);
		return env;
	}

}
