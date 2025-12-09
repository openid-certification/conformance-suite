package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthzenApiEndpointRequestAction extends CreateAuthzenApiEndpointRequestParameter {

	public CreateAuthzenApiEndpointRequestAction(JsonObject requestParameter) {
		super("resource", requestParameter);
		this.requiredProperties = new String[]{"name"};
		this.optionalObjects = new String[] {"properties"};
	}

	@Override
	@PostEnvironment(required = "authzen_api_endpoint_request_action")
	public Environment evaluate(Environment env) {
		JsonObject action = createAuthzenApiEndpointRequestParameter(env);
		env.putObject("authzen_api_endpoint_request_action", action);
		logSuccess("Created API action parameter", args(requestParameterName, action));
		return env;
	}
}
