package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class SetAccountScopeOnTokenEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request_form_parameters")
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject tokenEndpointRequest = env.getObject("token_endpoint_request_form_parameters");

		// overwrite anyhing that's already there
		tokenEndpointRequest.addProperty("scope", "accounts");

		logSuccess("Set scope parameter to accounts for OB testing", tokenEndpointRequest);

		env.putObject("token_endpoint_request_form_parameters", tokenEndpointRequest);

		return env;

	}

}
