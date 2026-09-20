package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetClientAssertionTypeToWrongValue extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_form_parameters")
	@PostEnvironment(required = "request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = env.getObject("request_form_parameters");

		o.addProperty("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:invalid");

		log("Set 'client_assertion_type' to a value that is not a registered assertion type, making the request invalid", o);

		return env;
	}

}
