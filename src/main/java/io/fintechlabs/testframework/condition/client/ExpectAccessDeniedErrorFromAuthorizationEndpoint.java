package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jheenan
 *
 */
public class ExpectAccessDeniedErrorFromAuthorizationEndpoint extends AbstractCondition {

	public ExpectAccessDeniedErrorFromAuthorizationEndpoint(String testId, TestInstanceEventLog log,
															ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "callback_params")
	public Environment evaluate(Environment env) {

		/* this error is in the fragment for code id_token and in the query in code */
		JsonObject callbackQueryParams = env.get("callback_params");

		if (!callbackQueryParams.has("error")) {
			throw error("error parameter not found", callbackQueryParams);
		}
		String error = callbackQueryParams.get("error").getAsString();
		if (Strings.isNullOrEmpty(error)) {
			throw error("error parameter empty/invalid", callbackQueryParams);
		}

		String expected = "access_denied";

		if (!error.equals(expected)) {
			throw error("error value is incorrect",
				args("expected", expected, "actual", error));
		}

		logSuccess("error parameter is correctly '"+error+"'");

		return env;
	}
}
