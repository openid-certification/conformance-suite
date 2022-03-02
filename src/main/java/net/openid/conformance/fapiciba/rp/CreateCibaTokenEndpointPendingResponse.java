package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateCibaTokenEndpointPendingResponse extends AbstractCondition {

	@Override
	//@PreEnvironment(strings = { "auth_req_id" }) // note the others are optional
	@PostEnvironment(required = { "token_endpoint_response" })
	public Environment evaluate(Environment env) {

		Integer pollCount = env.getInteger("token_poll_count");
		if(pollCount == null) {
			pollCount = 0;
		}
		pollCount++;
		env.putInteger("token_poll_count", pollCount);

		JsonObject tokenEndpointResponse = new JsonObject();
		tokenEndpointResponse.addProperty("error", "authorization_pending");

		env.putObject("token_endpoint_response", tokenEndpointResponse);

		logSuccess("Created token endpoint response", tokenEndpointResponse);

		return env;

	}

}
