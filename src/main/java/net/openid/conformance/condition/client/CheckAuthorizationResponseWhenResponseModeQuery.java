package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class CheckAuthorizationResponseWhenResponseModeQuery extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject callbackQueryParams = env.getObject("callback_query_params");
		JsonObject callbackFragmentParams = env.getObject("callback_params");

		if (callbackQueryParams.has("error")) {
			logSuccess("Server has chosen to return 'error' in URL query", args("error", env.getString("callback_query_params", "error")));
			env.mapKey("authorization_endpoint_response", "callback_query_params");
			return env;
		}

		if (callbackFragmentParams.has("error")) {
			logSuccess("Server has chosen to return 'error' in URL fragment", args("error", env.getString("callback_params", "error")));
			env.mapKey("authorization_endpoint_response", "callback_params");
			return env;
		}

		if (callbackFragmentParams.has("id_token")) {
			logSuccess("Authorization server ignores response_mode=query and return a successful response in the fragment");
			env.mapKey("authorization_endpoint_response", "callback_params");
			return env;
		}

		throw error("Authorization server returns a non-error response in the url query");
	}
}
