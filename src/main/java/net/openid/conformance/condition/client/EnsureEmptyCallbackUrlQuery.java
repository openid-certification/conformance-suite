package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureEmptyCallbackUrlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = "callback_query_params")
	public Environment evaluate(Environment env) {

		JsonObject callbackQueryParams = env.getObject("callback_query_params");

		if (callbackQueryParams.size() == 0) {
			logSuccess("URL query from server was correctly empty, no query parameters detected");
			return env;
		} else {
			throw error("URL query was not empty", args("callback_query_params", callbackQueryParams));
		}

	}

}
