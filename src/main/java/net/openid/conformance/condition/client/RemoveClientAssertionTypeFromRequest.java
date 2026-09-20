package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveClientAssertionTypeFromRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_form_parameters")
	@PostEnvironment(required = "request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = env.getObject("request_form_parameters");

		o.remove("client_assertion_type");

		log("Removed 'client_assertion_type' from the request, making it invalid", o);

		return env;
	}

}
