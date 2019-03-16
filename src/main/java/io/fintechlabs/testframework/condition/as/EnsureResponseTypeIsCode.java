package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureResponseTypeIsCode extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public EnsureResponseTypeIsCode(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		String responseType = env.getString("authorization_endpoint_request", "params.response_type");

		if (Strings.isNullOrEmpty(responseType)) {
			throw error("Could not find response type in request");
		} else if (!responseType.equals("code")) {
			throw error("Response type is not expected value", args("expected", "code", "actual", responseType));
		} else {
			logSuccess("Response type is expected value", args("expected", "code"));
			return env;
		}

	}

}
