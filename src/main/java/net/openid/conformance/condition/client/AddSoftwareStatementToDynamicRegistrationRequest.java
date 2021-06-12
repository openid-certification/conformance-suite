package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddSoftwareStatementToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request", "software_statement_assertion" })
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");
		String assertion = env.getString("software_statement_assertion", "value");

		dynamicRegistrationRequest.addProperty("software_statement", assertion);

		log("Added software_statement to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}
}
