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

	public static final int EXPIRES_IN = 180;

	@Override
	@PreEnvironment(required = { "backchannel_endpoint_http_request", "backchannel_request_object" })
	@PostEnvironment(required = "backchannel_endpoint_response", strings = "auth_req_id_expiration")
	public Environment evaluate(Environment env) {
		JsonObject backchannelResponse = new JsonObject();
		String authReqId = RFC6749AppendixASyntaxUtils.generateVSChar(40, 10, 0);
		env.putString("auth_req_id", authReqId); // Needed for the ping
		backchannelResponse.addProperty("auth_req_id", authReqId);
		backchannelResponse.addProperty("interval", 5); // CIBA 7.3 If no value is provided, clients MUST use 5 as the default value.

		Integer requestedExpiry = env.getInteger("backchannel_request_object", "claims.requested_expiry");
		int expiresIn = requestedExpiry != null ? requestedExpiry : EXPIRES_IN;
		backchannelResponse.addProperty("expires_in", expiresIn);

		String authReqIdExpiration = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(expiresIn));
		env.putString("auth_req_id_expiration", authReqIdExpiration);

		env.putObject("backchannel_endpoint_response", backchannelResponse);
		logSuccess("Created backchannel response", backchannelResponse);

		return env;
	}
}
