package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthzenApiEndpointRequestContext extends CreateAuthzenApiEndpointRequestParameter {

	public CreateAuthzenApiEndpointRequestContext(JsonObject requestParameter) {
		super("context", requestParameter);
		this.requiredProperties = new String[]{"name"};
		this.optionalObjects = new String[] {"properties"};
	}

	@Override
	@PostEnvironment(required = "authzen_api_endpoint_request_context")
	public Environment evaluate(Environment env) {
		JsonObject context = createAuthzenApiEndpointRequestWithAllParameters(env);
		env.putObject("authzen_api_endpoint_request_context", context);
		logSuccess("Created API context parameter", args(requestParameterName, context));
		return env;
	}
}
