package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckIfAccountRequestsEndpointResponseError extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CheckIfAccountRequestsEndpointResponseError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "account_requests_endpoint_response")
	public Environment evaluate(Environment env) {

		if (!Strings.isNullOrEmpty(env.getString("account_requests_endpoint_response", "error"))) {
			throw error("Account requests endpoint error response", env.getObject("account_requests_endpoint_response"));
		} else {
			logSuccess("No error from account requests endpoint");
			return env;
		}

	}

}
