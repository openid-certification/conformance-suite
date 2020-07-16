package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckSecondSessionResultIsChanged extends AbstractCondition {

	public static final String EXPECTED = "changed";

	@Override
	@PreEnvironment(required = "second_session_result")
	public Environment evaluate(Environment env) {
		String state = env.getString("second_session_result", "query_string_params.state");
		if (state == null) {
			throw error("state not present in the result from our iframe; this might be a bug in the test.");
		}

		if (!state.equals(EXPECTED)) {
			throw error("state from the OP's check_session_iframe does not have the expected value.",
				args("actual", state, "expected", EXPECTED));
		}

		logSuccess("state from the OP's check_session_iframe is '"+EXPECTED+"' as expected.");

		return env;

	}

}
