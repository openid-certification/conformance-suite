package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddFormBasedClientSecretToRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_form_parameters", "client" })
	@PostEnvironment(required = "request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = env.getObject("request_form_parameters");

		o.addProperty("client_id", env.getString("client", "client_id"));
		o.addProperty("client_secret", env.getString("client", "client_secret"));

		log(o);

		return env;
	}

}
