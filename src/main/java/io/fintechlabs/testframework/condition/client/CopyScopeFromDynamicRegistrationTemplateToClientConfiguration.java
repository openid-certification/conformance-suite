package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CopyScopeFromDynamicRegistrationTemplateToClientConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_client_registration_template")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		String scope = env.getString("dynamic_client_registration_template", "scope");
		JsonObject client = env.getObject("client");
		client.addProperty("scope", scope);
		env.putObject("client", client);

		log("Copied scope from dynamic_client_registration_template to client configuration", args("client", client));

		return env;
	}
}
