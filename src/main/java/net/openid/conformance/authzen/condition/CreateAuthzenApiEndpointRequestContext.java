package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthzenApiEndpointRequestContext extends CreateAuthzenApiEndpointRequestParameter {

	public CreateAuthzenApiEndpointRequestContext(JsonObject requestParameter) {
		super("context", requestParameter);
		this.requiredProperties = new String[]{"name"};
		this.optionalObjects = new String[] {"properties"};
	}

	protected JsonElement createAuthzenApiEndpointRequestParameter(Environment env) {
		if (requestParameter != null && requestParameter.isJsonObject()) {
			return requestParameter.getAsJsonObject().deepCopy();
		}
		return new JsonObject();
	}

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject context = createAuthzenApiEndpointRequestParameter(env).getAsJsonObject();
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		request.add("context", context);
		logSuccess("Created API context parameter", args(requestParameterName, context));
		return env;
	}
}
