package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForAccessTokenValue extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CheckForAccessTokenValue(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("token_endpoint_response", "access_token"))
			&& !Strings.isNullOrEmpty(env.getString("token_endpoint_response", "token_type"))) {

			logSuccess("Found an access token",
				args("access_token", env.getString("token_endpoint_response", "access_token")));
			return env;

		} else {
			throw error("Couldn't find required access token or token_type");
		}

	}

}
