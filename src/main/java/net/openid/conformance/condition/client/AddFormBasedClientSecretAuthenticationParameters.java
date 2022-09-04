package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
