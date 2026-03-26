package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthzenApiEndpointRequestAction extends CreateAuthzenApiEndpointRequestParameter {

	public CreateAuthzenApiEndpointRequestAction(JsonObject requestParameter) {
		super("action", requestParameter);
		this.requiredProperties = new String[]{"name"};
		this.optionalObjects = new String[] {"properties"};
	}

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject action = createAuthzenApiEndpointRequestParameter(env).getAsJsonObject();
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		request.add("action", action);
		logSuccess("Created API action parameter", args(requestParameterName, action));
		return env;
	}
}
