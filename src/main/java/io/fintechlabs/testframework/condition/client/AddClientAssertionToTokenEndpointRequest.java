package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddClientAssertionToTokenEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request_form_parameters", strings = "client_assertion")
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("token_endpoint_request_form_parameters")) {
			throw error("Couldn't find request form");
		}

		JsonObject o = env.getObject("token_endpoint_request_form_parameters");

		o.addProperty("client_assertion", env.getString("client_assertion"));
		o.addProperty("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

		env.putObject("token_endpoint_request_form_parameters", o);

		log("Added client assertion", o);

		return env;

	}

}
