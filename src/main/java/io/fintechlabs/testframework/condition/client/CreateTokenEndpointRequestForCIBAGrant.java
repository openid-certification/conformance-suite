package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreateTokenEndpointRequestForCIBAGrant extends AbstractCondition {

	@Override
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = new JsonObject();
		o.addProperty("grant_type", "urn:openid:params:grant-type:ciba");

		env.putObject("token_endpoint_request_form_parameters", o);

		logSuccess(o);

		return env;
	}

}
