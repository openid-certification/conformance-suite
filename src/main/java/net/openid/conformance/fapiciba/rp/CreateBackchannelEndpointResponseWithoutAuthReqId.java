package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateBackchannelEndpointResponseWithoutAuthReqId extends CreateBackchannelEndpointResponse {

	@Override
	@PreEnvironment(required = { "backchannel_endpoint_http_request", "backchannel_request_object" })
	@PostEnvironment(required = "backchannel_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject backchannelResponse = new JsonObject();

		addExpiresIn(env, backchannelResponse);

		env.putObject("backchannel_endpoint_response", backchannelResponse);
		logSuccess("Created backchannel response without auth_req_id", args("backchannel_endpoint_response", backchannelResponse));

		return env;
	}
}
