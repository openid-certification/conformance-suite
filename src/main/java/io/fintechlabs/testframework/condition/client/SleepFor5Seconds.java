package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.common.AbstractWaitForSpecifiedSeconds;
import io.fintechlabs.testframework.testmodule.Environment;

public class SleepFor5Seconds extends AbstractWaitForSpecifiedSeconds {

	@Override
	protected long getExpectedWaitSeconds(Environment env) {
		Long expiresIn = 5L;
		return expiresIn;
	}

}
