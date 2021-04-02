package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddFormBasedClientIdAuthenticationParameters extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "token_endpoint_request_form_parameters", "client" })
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {
		JsonObject o = env.getObject("token_endpoint_request_form_parameters");
		o.addProperty("client_id", env.getString("client", "client_id"));
		env.putObject("token_endpoint_request_form_parameters", o);

		log("Added client_id to token token_endpoint request", o);

		return env;
	}}
