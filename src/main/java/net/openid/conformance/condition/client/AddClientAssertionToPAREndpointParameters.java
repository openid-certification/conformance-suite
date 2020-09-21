package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddClientAssertionToPAREndpointParameters extends AbstractCondition {

	public static final String CLIENT_ASSERTION = "client_assertion";
	@Override
	@PreEnvironment(strings = {CLIENT_ASSERTION}, required = "pushed_authorization_request_form_parameters")
	@PostEnvironment(required = "pushed_authorization_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = env.getObject("pushed_authorization_request_form_parameters");

		o.addProperty(CLIENT_ASSERTION, env.getString("client_assertion"));
		o.addProperty("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

		logSuccess("Added client assertion to request", args("request", o));

		return env;
	}

}
