package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthzenApiEndpointSearchRequestPage extends CreateAuthzenApiEndpointRequestParameter {

	public CreateAuthzenApiEndpointSearchRequestPage(JsonObject requestParameter) {
		super("page", requestParameter);
		this.optionalProperties = new String[] {"token", "limit", "properties"};
	}

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject page = createAuthzenApiEndpointRequestParameter(env).getAsJsonObject();
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		request.add("page", page);
		logSuccess("Created Search API page parameter", args(requestParameterName, page));
		return env;
	}
}
