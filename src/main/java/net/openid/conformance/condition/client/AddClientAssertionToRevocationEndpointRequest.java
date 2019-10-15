package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddClientAssertionToRevocationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "revocation_endpoint_request_form_parameters", strings = "client_assertion")
	@PostEnvironment(required = "revocation_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("revocation_endpoint_request_form_parameters")) {
			throw error("Couldn't find request form");
		}

		JsonObject o = env.getObject("revocation_endpoint_request_form_parameters");

		o.addProperty("client_assertion", env.getString("client_assertion"));
		o.addProperty("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

		env.putObject("revocation_endpoint_request_form_parameters", o);

		log("Added client assertion", o);

		return env;

	}

}
