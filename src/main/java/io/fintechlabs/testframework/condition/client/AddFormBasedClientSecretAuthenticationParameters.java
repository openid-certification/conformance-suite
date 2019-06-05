package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddFormBasedClientSecretAuthenticationParameters extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "token_endpoint_request_form_parameters", "client" })
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("token_endpoint_request_form_parameters")) {
			throw error("Couldn't find request form");
		}

		JsonObject o = env.getObject("token_endpoint_request_form_parameters");

		o.addProperty("client_id", env.getString("client", "client_id"));
		o.addProperty("client_secret", env.getString("client", "client_secret"));

		env.putObject("token_endpoint_request_form_parameters", o);

		log(o);

		return env;
	}

}
