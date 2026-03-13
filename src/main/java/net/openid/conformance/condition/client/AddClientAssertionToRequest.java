package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddClientAssertionToRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_form_parameters", strings = "client_assertion")
	@PostEnvironment(required = "request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = env.getObject("request_form_parameters");

		o.addProperty("client_assertion", env.getString("client_assertion"));
		o.addProperty("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

		log("Added client assertion", o);

		return env;

	}

}
