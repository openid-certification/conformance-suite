package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckAuthorizationResponseWhenResponseModeQuery extends AbstractCondition {

	public CheckAuthorizationResponseWhenResponseModeQuery(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	public Environment evaluate(Environment env) {
		JsonObject callbackQueryParams = env.getObject("callback_query_params");
		JsonObject callbackFragmentParams = env.getObject("callback_params");

		if (callbackQueryParams.has("error")) {
			logSuccess("'error' is returned in URL query", args("error", env.getString("callback_query_params", "error")));
			env.mapKey("authorization_endpoint_response", "callback_query_params");
			return env;
		}

		if (callbackFragmentParams.has("error")) {
			logSuccess("'error' is returned in URL fragment", args("error", env.getString("callback_params", "error")));
			return env;
		}

		if (callbackFragmentParams.has("id_token")) {
			logSuccess("Authorisation server ignores response_mode=query and return a successful response in the fragment");
			return env;
		}

		throw error("Authorisation server returns a non-error response in the url query");
	}
}
