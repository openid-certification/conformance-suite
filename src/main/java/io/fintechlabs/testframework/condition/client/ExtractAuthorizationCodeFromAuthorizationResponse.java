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
public class ExtractAuthorizationCodeFromAuthorizationResponse extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public ExtractAuthorizationCodeFromAuthorizationResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "callback_params")
	public Environment evaluate(Environment in) {
		if (Strings.isNullOrEmpty(in.getString("callback_params", "code"))) {
			throw error("Couldn't find authorization code in callback");
		} else {
			in.putString("code", in.getString("callback_params", "code"));
			logSuccess("Found authorization code",
				args("code", in.getString("callback_params", "code")));
			return in;
		}

	}

}
