package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateEmptyAuthzenApiEndpointRequest extends AbstractCondition {

	@Override
	@PostEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authzenApiRequest = new JsonObject();
		env.putObject("authzen_api_endpoint_request", authzenApiRequest);
		env.removeObject("authzen_api_endpoint_request_headers");
		logSuccess("Created empty authorization endpoint request", authzenApiRequest);
		return env;
	}

}
