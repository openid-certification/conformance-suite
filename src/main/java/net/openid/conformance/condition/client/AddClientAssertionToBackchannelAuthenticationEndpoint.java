package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
