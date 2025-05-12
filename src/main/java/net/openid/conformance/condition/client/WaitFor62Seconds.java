package net.openid.conformance.condition.client;

import net.openid.conformance.condition.common.AbstractWaitForSpecifiedSeconds;
import net.openid.conformance.testmodule.Environment;

public class WaitFor62Seconds extends AbstractWaitForSpecifiedSeconds {

	@Override
	protected long getExpectedWaitSeconds(Environment env) {
		return 62;
	}
}
