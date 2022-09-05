package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BuildUnsignedPAREndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "pushed_authorization_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		JsonObject parRequest = authorizationEndpointRequest.deepCopy();

		env.putObject("pushed_authorization_request_form_parameters", parRequest);

		logSuccess("Created PAR endpoint request", parRequest);

		return env;
	}

}
