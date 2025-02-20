package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractBrowserApiAuthorizationEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "original_authorization_endpoint_response")
	@PostEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject response = env.getObject("original_authorization_endpoint_response");
		env.putObject("authorization_endpoint_response", response);

		logSuccess("Extracted authorization response", response);

		return env;
	}

}
