package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetConsentsScopeOnTokenEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request_form_parameters")
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject tokenEndpointRequest = env.getObject("token_endpoint_request_form_parameters");

		// overwrite anything that's already there
		tokenEndpointRequest.addProperty("scope", "consents");

		logSuccess("Set scope parameter to 'consents'", tokenEndpointRequest);

		env.putObject("token_endpoint_request_form_parameters", tokenEndpointRequest);

		return env;

	}

}
