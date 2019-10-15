package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateBackchannelAuthenticationEndpointRequest extends AbstractCondition {

	@Override
	@PostEnvironment(required = "backchannel_authentication_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = new JsonObject();

		env.putObject("backchannel_authentication_endpoint_request_form_parameters", o);

		logSuccess("Created backchannel authentication endpoint request", o);

		return env;
	}

}
