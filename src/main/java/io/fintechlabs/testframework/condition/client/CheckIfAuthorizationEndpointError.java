package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * Check if there was an error from the authorization endpoint. If so, log the error and quit. If not, pass.
 *
 * @author jricher
 *
 */
public class CheckIfAuthorizationEndpointError extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CheckIfAuthorizationEndpointError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "callback_params")
	public Environment evaluate(Environment in) {
		if (!in.containsObject("callback_params")) {
			throw error("Couldn't find callback parameters");
		}

		if (!Strings.isNullOrEmpty(in.getString("callback_params", "error"))) {
			throw error("Error from the authorization endpoint", in.getObject("callback_params"));
		} else {
			logSuccess("No error from authorization endpoint");
			return in;
		}

	}

}
