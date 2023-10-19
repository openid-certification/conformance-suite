package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class CreateBackchannelEndpointResponse extends AbstractCondition {

	public static final int EXPIRES_IN = 300;

	@Override
	@PreEnvironment(required = { "backchannel_endpoint_http_request", "backchannel_request_object" })
	@PostEnvironment(required = "backchannel_endpoint_response", strings = { "auth_req_id", "auth_req_id_expiration" })
	public Environment evaluate(Environment env) {
		JsonObject backchannelResponse = new JsonObject();

		addAuthReqId(env, backchannelResponse);
		addExpiresIn(env, backchannelResponse);
		addInterval(env, backchannelResponse);

		env.putObject("backchannel_endpoint_response", backchannelResponse);
		logSuccess("Created backchannel response", args("backchannel_endpoint_response", backchannelResponse));

		return env;
	}

	protected void addExpiresIn(Environment env, JsonObject backchannelResponse) {
		Integer requestedExpiry = env.getInteger("requested_expiry");
		int expiresIn = requestedExpiry != null ? requestedExpiry : EXPIRES_IN;
		backchannelResponse.addProperty("expires_in", expiresIn);
		log("Set expires_in", args("expires_in", expiresIn));

		String authReqIdExpiration = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(expiresIn));
		env.putString("auth_req_id_expiration", authReqIdExpiration);
	}

	protected void addAuthReqId(Environment env, JsonObject backchannelResponse) {
		String authReqId = RFC6749AppendixASyntaxUtils.generateVSChar(40, 10, 0);
		env.putString("auth_req_id", authReqId);
		backchannelResponse.addProperty("auth_req_id", authReqId);
		log("Set auth_req_id", args("auth_req_id", authReqId));
	}

	protected void addInterval(Environment env, JsonObject backchannelResponse) {
		Integer interval = env.getInteger("interval");
		if (interval != null) {
			backchannelResponse.addProperty("interval", interval);
		}
		log("Set interval", args("interval", interval));
	}

}
