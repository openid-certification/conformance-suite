package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateTokenEndpointRequestForAuthorizationCodeGrant extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "code", "redirect_uri" })
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = new JsonObject();
		o.addProperty("grant_type", "authorization_code");
		o.addProperty("code", env.getString("code"));
		o.addProperty("redirect_uri", env.getString("redirect_uri"));

		env.putObject("token_endpoint_request_form_parameters", o);

		// Remove headers as well, so that we're truly starting a 'new' request
		env.removeObject("token_endpoint_request_headers");

		logSuccess("Created token endpoint request", o);

		return env;
	}

}
