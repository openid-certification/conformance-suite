package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class RejectErrorInUrlQuery extends AbstractCondition {

	public RejectErrorInUrlQuery(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("callback_query_params", "error"))) {
			throw error("'error' is present in URL query returned from authorization endpoint - it should be returned in the URL fragment only");
		}

		logSuccess("'error' is not present in URL query returned from authorization endpoint");
		return env;
	}
}
