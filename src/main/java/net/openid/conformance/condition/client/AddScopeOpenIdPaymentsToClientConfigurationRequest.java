package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddScopeOpenIdPaymentsToClientConfigurationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "registration_client_endpoint_request_body" })
	@PostEnvironment(required = "registration_client_endpoint_request_body")
	public Environment evaluate(Environment env) {

		JsonObject request = env.getObject("registration_client_endpoint_request_body");

		request.addProperty("scope", "openid payments");

		log("Added scope 'openid payments' to client configuration request", args("client_configuration_request", request));

		return env;
	}
}
