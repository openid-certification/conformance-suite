package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class EnsureInvalidRequestError extends AbstractCondition {

	public EnsureInvalidRequestError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "callback_query_params")
	public Environment evaluate(Environment env) {

		String error = env.getString("callback_query_params", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("Expected 'error' field not found");
		} else if (!error.equals("invalid_request")) {
			throw error("'error' field has unexpected value", args("expected", "invalid_request", "actual", error));
		} else {
			logSuccess("Authorization endpoint returned expected 'error' of 'invalid_request'", args("error", error));

			return env;
		}

	}


}
