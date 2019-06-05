package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class ExpectAccessDeniedErrorFromAuthorizationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "callback_params")
	public Environment evaluate(Environment env) {

		/* this error is in the fragment for code id_token and in the query in code */
		JsonObject callbackQueryParams = env.getObject("callback_params");

		if (!callbackQueryParams.has("error")) {
			throw error("error parameter not found", callbackQueryParams);
		}
		String error = OIDFJSON.getString(callbackQueryParams.get("error"));
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
