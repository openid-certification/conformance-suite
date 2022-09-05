package net.openid.conformance.condition.as.par;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class CreatePAREndpointResponse extends AbstractCondition {

	public static final int EXPIRES_IN = 600;

	@Override
	@PreEnvironment()
	@PostEnvironment(required = "par_endpoint_response", strings = "par_endpoint_generated_request_uri")
	public Environment evaluate(Environment env) {
		String requestUri = "urn:ietf:params:oauth:request_uri:" + UUID.randomUUID().toString();
		env.putString("par_endpoint_generated_request_uri", requestUri);

		JsonObject parEndpointResponse = new JsonObject();
		parEndpointResponse.addProperty("request_uri", requestUri);
		parEndpointResponse.addProperty("expires_in", EXPIRES_IN);

		env.putObject("par_endpoint_response", parEndpointResponse);

		logSuccess("Created PAR endpoint response", parEndpointResponse);

		return env;
	}
}
