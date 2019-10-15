package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddScopeToTokenEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "token_endpoint_request_form_parameters", "client" } )
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {
		JsonObject tokenEndpointRequest = env.getObject("token_endpoint_request_form_parameters");

		String scope = env.getString("client", "scope");
		if (Strings.isNullOrEmpty(scope)) {
			throw error("scope missing/empty in client object");
		}

		tokenEndpointRequest.addProperty("scope", scope);

		env.putObject("token_endpoint_request_form_parameters", tokenEndpointRequest);

		logSuccess("Added scope of '"+scope+"' to token endpoint request", tokenEndpointRequest);

		return env;
	}
}
