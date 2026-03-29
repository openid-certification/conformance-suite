package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthzenApiEndpointRequestOptions extends CreateAuthzenApiEndpointRequestParameter {

	public CreateAuthzenApiEndpointRequestOptions(JsonObject requestParameter) {
		super("options", requestParameter);
		this.requiredProperties = new String[]{};
		this.optionalObjects = new String[] {};
	}

	@Override
	protected JsonElement createAuthzenApiEndpointRequestParameter(Environment env) {
		if (requestParameter != null && requestParameter.isJsonObject()) {
			return requestParameter.getAsJsonObject().deepCopy();
		}
		return new JsonObject();
	}

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_request_options")
	public Environment evaluate(Environment env) {
		JsonObject options = createAuthzenApiEndpointRequestParameter(env).getAsJsonObject();
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		request.add("options", options);
		logSuccess("Created API options parameter", args(requestParameterName, options));
		return env;
	}
}
