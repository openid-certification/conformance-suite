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
public class ExpectInvalidRequestObjectError extends AbstractCondition {

	public ExpectInvalidRequestObjectError(String testId, TestInstanceEventLog log,
										   ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "callback_query_params")
	public Environment evaluate(Environment env) {

		/* request object errors are always in the query section of the url, even in hybrid flow */
		JsonObject callbackQueryParams = env.getObject("callback_query_params");

		if (!callbackQueryParams.has("error")) {
			throw error("error parameter not found", callbackQueryParams);
		}
		String error = callbackQueryParams.get("error").getAsString();
		if (Strings.isNullOrEmpty(error)) {
			throw error("error parameter empty/invalid", callbackQueryParams);
		}

		String expected = "invalid_request_object";

		if (!error.equals(expected)) {
			throw error("error value is incorrect",
				args("Expected", expected, "Actual", error));
		}

		logSuccess("error parameter is correctly '"+error+"'");

		return env;
	}
}
