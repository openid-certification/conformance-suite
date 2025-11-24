package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthzenApiEndpointRequestSubject extends CreateAuthzenApiEndpointRequestParameter {

	public CreateAuthzenApiEndpointRequestSubject(JsonObject requestParameter) {
		super("subject", requestParameter);
		this.requiredProperties = new String[]{"type", "id"};
		this.optionalObjects = new String[] {"properties"};
	}

	@Override
	@PostEnvironment(required = "authzen_api_endpoint_request_subject")
	public Environment evaluate(Environment env) {
		JsonObject subject = createAuthzenApiEndpointRequestParameter(env);
		env.putObject("authzen_api_endpoint_request_subject", subject);
		logSuccess("Created API subject parameter", args(requestParameterName, subject));
		return env;
	}
}
