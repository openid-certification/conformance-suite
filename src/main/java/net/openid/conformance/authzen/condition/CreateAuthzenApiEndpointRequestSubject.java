package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthzenApiEndpointRequestSubject extends CreateAuthzenApiEndpointRequestParameter {

	public CreateAuthzenApiEndpointRequestSubject(JsonObject requestParameter) {
		super("subject", requestParameter);
		this.requiredProperties = new String[]{"type", "id"};
		this.optionalProperties = new String[] {"properties"};
	}

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject subject = createAuthzenApiEndpointRequestParameter(env).getAsJsonObject();
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		request.add("subject", subject);
		logSuccess("Created API subject parameter", args(requestParameterName, subject));
		return env;
	}
}
