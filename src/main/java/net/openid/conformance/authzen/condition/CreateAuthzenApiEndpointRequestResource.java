package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthzenApiEndpointRequestResource extends CreateAuthzenApiEndpointRequestParameter {

	public CreateAuthzenApiEndpointRequestResource(JsonObject requestParameter) {
		super("resource", requestParameter);
		this.requiredProperties = new String[]{"type", "id"};
		this.optionalObjects = new String[] {"properties"};
	}

	@Override
	@PostEnvironment(required = "authzen_api_endpoint_request_resource")
	public Environment evaluate(Environment env) {
		JsonObject resource = createAuthzenApiEndpointRequestParameter(env);
		env.putObject("authzen_api_endpoint_request_resource", resource);
		logSuccess("Created API resource parameter", args(requestParameterName, resource));
		return env;
	}
}
