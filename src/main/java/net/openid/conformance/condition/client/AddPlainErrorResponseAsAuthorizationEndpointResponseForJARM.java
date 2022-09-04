package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddPlainErrorResponseAsAuthorizationEndpointResponseForJARM extends AbstractCondition {

	@Override
	@PreEnvironment(required = "callback_query_params")
	@PostEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject callbackQueryParams = env.getObject("callback_query_params");
		env.mapKey("authorization_endpoint_response", "callback_query_params");

		logSuccess("The server returned a plain error response instead of a JARM response", callbackQueryParams);

		return env;
	}

}
