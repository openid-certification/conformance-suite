package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

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
