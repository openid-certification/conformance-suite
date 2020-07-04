package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateEmptyResourceEndpointRequestHeaders extends AbstractCondition {

	@Override
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject headers = new JsonObject();

		env.putObject("resource_endpoint_request_headers", headers);

		log("Created empty headers", args("resource_endpoint_request_headers", headers));

		return env;
	}

}
