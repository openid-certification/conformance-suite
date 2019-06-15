package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.common.AbstractWaitForSpecifiedSeconds;
import io.fintechlabs.testframework.testmodule.Environment;

public class WaitForOneSecond extends AbstractWaitForSpecifiedSeconds {

	@Override
	protected long getExpectedWaitSeconds(Environment env) {
		return 1;
	}
}
