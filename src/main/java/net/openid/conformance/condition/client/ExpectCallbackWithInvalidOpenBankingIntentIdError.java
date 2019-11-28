package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ExpectCallbackWithInvalidOpenBankingIntentIdError extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		createBrowserInteractionPlaceholder("The client must show an error message that the openbanking_intent_id returned in id_token from authorization endpoint does not match the value sent in the request object - upload a log file or screenshot of the error.");
		return env;
	}

}
