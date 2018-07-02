package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForAuthorizationEndpointErrorInQueryForHybridFLow extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CheckForAuthorizationEndpointErrorInQueryForHybridFLow(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}
	
	
	@Override
	@PreEnvironment(required = "callback_query_params")
	public Environment evaluate(Environment in) {

		if (!Strings.isNullOrEmpty(in.getString("callback_query_params", "error"))) {
			throw error("Error from the authorization endpoint", in.get("callback_query_params"));
		} else {
			logSuccess("No error in callback_query_params from authorization endpoint");
			return in;
		}

	}

}
