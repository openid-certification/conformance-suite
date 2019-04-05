package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class RejectAuthCodeInUrlQuery extends AbstractCondition {

	public RejectAuthCodeInUrlQuery(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment() // We want an explicit error if implicit_hash is empty
	@PostEnvironment(required = "callback_query_params")
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("callback_query_params", "code"))) {
			throw error("Authorization code is present in URL query returned from authorization endpoint - hybrid/implicit flow require it to be returned in the URL fragment/hash only");
		}

		logSuccess("Authorization code is not present in URL query returned from authorization endpoint");
		return env;
	}

}
