package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddClientAssertionToBackchannelAuthenticationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_request_form_parameters", strings = "client_assertion" )
	@PostEnvironment(required = "backchannel_authentication_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = env.getObject("backchannel_authentication_endpoint_request_form_parameters");

		o.addProperty("client_assertion", env.getString("client_assertion"));
		o.addProperty("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

		env.putObject("backchannel_authentication_endpoint_request_form_parameters", o);

		log("Added client assertion", o);

		return env;

	}

}
