package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthzenApiEndpointRequestEvaluations extends CreateAuthzenApiEndpointRequestParameter {

	public CreateAuthzenApiEndpointRequestEvaluations(JsonArray requestParameter) {
		super("evaluations", requestParameter);
		this.requiredProperties = new String[]{};
		this.optionalObjects = new String[] {};
	}

	@Override
	protected JsonElement createAuthzenApiEndpointRequestParameter(Environment env) {
		if (requestParameter != null && requestParameter.isJsonArray()) {
			return requestParameter.getAsJsonArray().deepCopy();
		}
		return new JsonArray();
	}

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonArray evaluations = createAuthzenApiEndpointRequestParameter(env).getAsJsonArray();
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		request.add("evaluations", evaluations);
		logSuccess("Created API evaluations parameter", args(requestParameterName, evaluations));
		return env;
	}
}
