package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.List;

public class ExpectExpiredTokenErrorFromTokenEndpoint extends AbstractCondition {

	public ExpectExpiredTokenErrorFromTokenEndpoint(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment( required = "token_endpoint_response" )
	public Environment evaluate(Environment env) {
		String error = env.getString("token_endpoint_response", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("error missing from token_endpoint_response.");
		}

		String expected = "expired_token";

		if (!error.equals(expected)) {
			throw error("error value is incorrect",
				args("expected", expected, "actual", error));
		}

		logSuccess("error parameter is correctly '"+error+"'");

		return env;
	}
}
