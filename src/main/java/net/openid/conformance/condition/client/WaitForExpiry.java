package net.openid.conformance.condition.client;

import net.openid.conformance.condition.common.AbstractWaitForSpecifiedSeconds;
import net.openid.conformance.testmodule.Environment;

public class WaitForExpiry extends AbstractWaitForSpecifiedSeconds {

	@Override
	protected long getExpectedWaitSeconds(Environment env) {
		//expires_in : A JSON number that represents the lifetime of the request URI in seconds.
		// The request URI lifetime is at the discretion of the AS.
		Long seconds = env.getLong("expires_in");
		if (seconds == null) {
			throw error("Missing key expires_in");
		}
		return seconds;
	}
}
