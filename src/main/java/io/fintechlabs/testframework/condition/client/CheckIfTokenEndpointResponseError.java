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
public class CheckIfTokenEndpointResponseError extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CheckIfTokenEndpointResponseError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("token_endpoint_response")) {
			throw error("Couldn't find token endpoint response");
		}

		if (!Strings.isNullOrEmpty(env.getString("token_endpoint_response", "error"))) {
			throw error("Token endpoint error response", env.getObject("token_endpoint_response"));
		} else {
			logSuccess("No error from token endpoint");
			return env;
		}

	}

}
