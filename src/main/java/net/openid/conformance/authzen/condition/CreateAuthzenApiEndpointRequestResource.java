package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthzenApiEndpointRequestResource extends CreateAuthzenApiEndpointRequestParameter {

	public CreateAuthzenApiEndpointRequestResource(JsonObject requestParameter) {
		super("resource", requestParameter);
		this.requiredProperties = new String[]{"type", "id"};
		this.optionalProperties = new String[] {"properties"};
	}

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject resource = createAuthzenApiEndpointRequestParameter(env).getAsJsonObject();
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		request.add("resource", resource);
		logSuccess("Created API resource parameter", args(requestParameterName, resource));
		return env;
	}
}
