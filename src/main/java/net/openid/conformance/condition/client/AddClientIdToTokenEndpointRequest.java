package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddClientIdToTokenEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "token_endpoint_request_form_parameters", "client" })
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = env.getObject("token_endpoint_request_form_parameters");

		String clientId = env.getString("client", "client_id");
		if (Strings.isNullOrEmpty(clientId)) {
			throw error("client_id is null or empty");
		}

		o.addProperty("client_id", clientId);

		log(o);

		return env;

	}

}
