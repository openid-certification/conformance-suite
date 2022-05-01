package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.testmodule.Environment;

public class CreateBackchannelEndpointResponse extends AbstractCondition {

	public static final int EXPIRES_IN = 180;

	@Override
	@PreEnvironment(required = "backchannel_endpoint_http_request")
	@PostEnvironment(required = "backchannel_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject backchannelResponse = new JsonObject();
		String authReqId = RFC6749AppendixASyntaxUtils.generateVSChar(40, 10, 0);
		env.putString("auth_req_id", authReqId); // Needed for the ping
		backchannelResponse.addProperty("auth_req_id", authReqId);
		backchannelResponse.addProperty("interval", 5);

		String requestedExpiryString = env.getString("backchannel_endpoint_http_request", "body_form_params.requested_expiry");
		int expiresIn = getIntValueOrDefault(requestedExpiryString, EXPIRES_IN);
		backchannelResponse.addProperty("expires_in", expiresIn);

		env.putObject("backchannel_endpoint_response", backchannelResponse);

		return env;
	}

	protected static int getIntValueOrDefault(String intString, int defaultValue) {
		try {
			return Integer.parseInt(intString);
		} catch (NumberFormatException nfe) {
			return defaultValue;
		}
	}
}
