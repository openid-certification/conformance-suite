package net.openid.conformance.condition.client;

import net.openid.conformance.condition.common.AbstractWaitForSpecifiedSeconds;
import net.openid.conformance.testmodule.Environment;

public class WaitForConfiguredSeconds extends AbstractWaitForSpecifiedSeconds {

	@Override
	protected long getExpectedWaitSeconds(Environment env) {
		int pauseTime =  env.getInteger("loopSequencePauseTime");
		logSuccess("Loop sequence configured to pause for " + pauseTime + " seconds");
		return pauseTime;
	}
}
