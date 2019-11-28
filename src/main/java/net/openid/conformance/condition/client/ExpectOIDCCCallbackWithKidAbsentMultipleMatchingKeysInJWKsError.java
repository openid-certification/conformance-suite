package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ExpectOIDCCCallbackWithKidAbsentMultipleMatchingKeysInJWKsError extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		createBrowserInteractionPlaceholder("The client show an error message that multiple matching keys found in issuer's jwks_uri, kid must provided  - upload a log file or screenshot of the error.");
		return env;
	}

}
