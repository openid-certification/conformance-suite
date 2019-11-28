package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ExpectCallbackWithInvalidSHashError extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		createBrowserInteractionPlaceholder("The client must show an error message that the s_hash value in the id_token does not match the state the client sent - upload a log file or screenshot of the error.");
		return env;
	}

}
