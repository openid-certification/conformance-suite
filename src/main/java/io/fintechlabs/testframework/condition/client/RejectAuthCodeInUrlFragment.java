package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class RejectAuthCodeInUrlFragment extends AbstractCondition {

	public RejectAuthCodeInUrlFragment(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "callback_params")
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("callback_params", "code"))) {
			throw error("Authorization code is present in URL fragment returned from authorization endpoint");
		}

		logSuccess("Authorization code is not present in URL fragment returned from authorization endpoint");
		return env;
	}

}
