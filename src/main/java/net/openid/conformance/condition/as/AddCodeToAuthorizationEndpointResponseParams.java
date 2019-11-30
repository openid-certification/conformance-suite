package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddCodeToAuthorizationEndpointResponseParams extends AbstractCondition {

	private static final String AUTHORIZATION_ENDPOINT_RESPONSE_PARAMS = "authorization_endpoint_response_params";
	@Override
	@PreEnvironment(required = AUTHORIZATION_ENDPOINT_RESPONSE_PARAMS, strings = "authorization_code")
	@PostEnvironment(required = AUTHORIZATION_ENDPOINT_RESPONSE_PARAMS)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(AUTHORIZATION_ENDPOINT_RESPONSE_PARAMS);

		String code = env.getString("authorization_code");

		params.addProperty("code", code);

		env.putObject(AUTHORIZATION_ENDPOINT_RESPONSE_PARAMS, params);

		logSuccess("Added code to authorization endpoint response params", args(AUTHORIZATION_ENDPOINT_RESPONSE_PARAMS, params));

		return env;

	}

}
