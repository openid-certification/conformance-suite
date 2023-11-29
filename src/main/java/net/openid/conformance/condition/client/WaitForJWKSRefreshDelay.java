package net.openid.conformance.condition.client;

import net.openid.conformance.condition.common.AbstractWaitForSpecifiedSeconds;
import net.openid.conformance.testmodule.Environment;

public class WaitForJWKSRefreshDelay extends AbstractWaitForSpecifiedSeconds {

	@Override
	protected long getExpectedWaitSeconds(Environment env) {
		long defaultRefreshDelay = 60;
		Long customRefreshDelay  = env.getLong("config", "server.jwks_refresh_delay");

		if (customRefreshDelay == null) {
			return defaultRefreshDelay;
		}

		// The custom delay must be in the range 1..defaultRefreshDelay
		return Long.min(Long.max(1, customRefreshDelay.longValue()), defaultRefreshDelay);
	}
}
