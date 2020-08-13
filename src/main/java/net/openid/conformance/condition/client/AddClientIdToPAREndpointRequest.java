package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddClientIdToPAREndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "pushed_authorization_request_form_parameters", "client" })
	@PostEnvironment(required = "pushed_authorization_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = env.getObject("pushed_authorization_request_form_parameters");

		String clientId = env.getString("client", "client_id");

		if (clientId == null) {
			throw error("missing client_id in environment");
		}

		o.addProperty("client_id", clientId);

		env.putObject("pushed_authorization_request_form_parameters", o);

		logSuccess(o);

		return env;
	}
}
