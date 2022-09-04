package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class LogAccessTokenAlwaysRejectedToForceARefreshGrant extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		logSuccess("This call will be always rejected. The client must obtain a new access token twice using the refresh_token");
		return env;

	}

}
