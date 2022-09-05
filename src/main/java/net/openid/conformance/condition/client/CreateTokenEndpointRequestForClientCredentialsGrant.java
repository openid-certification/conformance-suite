package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateTokenEndpointRequestForClientCredentialsGrant extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = new JsonObject();
		o.addProperty("grant_type", "client_credentials");

		// add the scope if it exists
		String scope = env.getString("client", "scope");
		if (!Strings.isNullOrEmpty(scope)) {
			o.addProperty("scope", scope);
		} else {
			log("Leaving off 'scope' parameter from token request");
		}

		env.putObject("token_endpoint_request_form_parameters", o);

		// Remove headers as well, so that we're truly starting a 'new' request
		env.removeObject("token_endpoint_request_headers");

		logSuccess("Created token endpoint request", o);

		return env;
	}

}
