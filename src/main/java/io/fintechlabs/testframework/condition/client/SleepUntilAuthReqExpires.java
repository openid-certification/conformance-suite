package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.common.AbstractWaitForSpecifiedSeconds;
import io.fintechlabs.testframework.testmodule.Environment;

public class SleepUntilAuthReqExpires extends AbstractWaitForSpecifiedSeconds {

	@Override
	protected long getExpectedWaitSeconds(Environment env) {
		return env.getLong("backchannel_authentication_endpoint_response", "expires_in");
	}

}
