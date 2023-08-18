package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class WarningAboutTestingOldSpec extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("Testing pre-ID2 behaviour of direct_post due to pre_id2: true in test configuration");
	}

}
