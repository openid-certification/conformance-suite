package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class DetectWhetherErrorResponseIsInQueryOrFragment extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject callbackQueryParams = env.getObject("callback_query_params");
		JsonObject callbackFragmentParams = env.getObject("callback_params");

		if (callbackQueryParams.has("error")) {
			log("Server has chosen to return 'error' in URL query, using query as response", args("error", env.getString("callback_query_params", "error")));
			env.mapKey("authorization_endpoint_response", "callback_query_params");
			return env;
		}
		if (callbackFragmentParams.has("error")) {
			log("Server has chosen to return 'error' in URL fragment, using fragment as the response", args("error", env.getString("callback_params", "error")));
			env.mapKey("authorization_endpoint_response", "callback_params");
			return env;
		}

		throw error("Authorisation server did not return an error in the url query or fragment");
	}
}
