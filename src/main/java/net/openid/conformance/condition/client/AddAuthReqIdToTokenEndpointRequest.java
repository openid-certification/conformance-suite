package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddAuthReqIdToTokenEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "token_endpoint_request_form_parameters", "backchannel_authentication_endpoint_response" })
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = env.getObject("token_endpoint_request_form_parameters");

		final String authReqId = env.getString("backchannel_authentication_endpoint_response", "auth_req_id");

		if (authReqId == null) {
			throw error("auth_req_id missing from backchannel_authentication_endpoint_response");
		}

		o.addProperty("auth_req_id", authReqId);

		env.putObject("token_endpoint_request_form_parameters", o);

		log(o);

		return env;

	}

}
