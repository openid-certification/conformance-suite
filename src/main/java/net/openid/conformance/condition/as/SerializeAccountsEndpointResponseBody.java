package net.openid.conformance.condition.as;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Serializes the accounts endpoint response once, so that the body sent is exactly the body a
 * response signature covers. The accounts endpoint sends {@link #ACCOUNTS_RESPONSE_BODY}, when present,
 * as is instead of serializing the JSON object itself.
 */
public class SerializeAccountsEndpointResponseBody extends AbstractCondition {

	public static final String ACCOUNTS_RESPONSE_BODY = "accounts_endpoint_response_body";

	@Override
	@PreEnvironment(required = "accounts_endpoint_response")
	@PostEnvironment(strings = ACCOUNTS_RESPONSE_BODY)
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("accounts_endpoint_response");
		String body = new Gson().toJson(response);
		env.putString(ACCOUNTS_RESPONSE_BODY, body);
		logSuccess("Serialized the accounts endpoint response body", args("body", body));
		return env;
	}
}
