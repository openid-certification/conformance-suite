package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class CheckUrlQueryIsEmpty extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject queryParams = env.getObject("callback_query_params");

		if (queryParams.size() != 0) {
			throw error("URL query passed to redirect_uri - it should be empty", queryParams);
		}

		logSuccess("URL query passed to redirect_uri is empty");

		return env;
	}
}
