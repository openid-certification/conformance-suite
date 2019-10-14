package net.openid.conformance.condition.client;

import net.openid.conformance.condition.common.AbstractWaitForSpecifiedSeconds;
import net.openid.conformance.testmodule.Environment;

public class SleepUntilAuthReqExpires extends AbstractWaitForSpecifiedSeconds {

	@Override
	protected long getExpectedWaitSeconds(Environment env) {
		return env.getLong("backchannel_authentication_endpoint_response", "expires_in");
	}

}
