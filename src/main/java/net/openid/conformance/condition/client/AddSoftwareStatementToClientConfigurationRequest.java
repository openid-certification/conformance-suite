package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddSoftwareStatementToClientConfigurationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "registration_client_endpoint_request_body", "software_statement_assertion" })
	@PostEnvironment(required = "registration_client_endpoint_request_body")
	public Environment evaluate(Environment env) {

		JsonObject request = env.getObject("registration_client_endpoint_request_body");
		String assertion = env.getString("software_statement_assertion", "value");

		request.addProperty("software_statement", assertion);

		log("Added software_statement to client configuration request", args("client_configuration_request", request));

		return env;
	}
}
