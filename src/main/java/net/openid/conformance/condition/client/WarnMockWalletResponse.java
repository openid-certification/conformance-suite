package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Always fails with a message indicating that a mock wallet response is being used.
 * Called with ConditionResult.FAILURE so the result appears in the test log; the
 * integration test expected-failures configuration should list this condition.
 */
public class WarnMockWalletResponse extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("'mock_wallet_response' is set in the test configuration - using a mock wallet response, not testing a real wallet implementation");
	}

}
