package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddCodeToAuthorizationEndpointResponseParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response_params", strings = "authorization_code")
	@PostEnvironment(required = "authorization_endpoint_response_params")
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject("authorization_endpoint_response_params");

		String code = env.getString("authorization_code");

		params.addProperty("code", code);

		env.putObject("authorization_endpoint_response_params", params);

		logSuccess("Added code to authorization endpoint response params", args("authorization_endpoint_response_params", params));

		return env;

	}

}
