package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateBackchannelEndpointResponseWithLongInterval extends CreateBackchannelEndpointResponse {

	public static final int INTERVAL = 31;

	@Override
	@PreEnvironment(required = { "backchannel_endpoint_http_request", "backchannel_request_object" })
	@PostEnvironment(required = "backchannel_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject backchannelResponse = new JsonObject();

		addAuthReqId(env, backchannelResponse);
		addExpiresIn(env, backchannelResponse);

		addInterval(env, backchannelResponse);

		env.putObject("backchannel_endpoint_response", backchannelResponse);
		logSuccess("Created backchannel response with long interval", args("backchannel_endpoint_response", backchannelResponse));

		return env;
	}

	protected void addInterval(Environment env, JsonObject backchannelResponse) {
		backchannelResponse.addProperty("interval", INTERVAL);
		env.putInteger("interval", INTERVAL);
	}

}
