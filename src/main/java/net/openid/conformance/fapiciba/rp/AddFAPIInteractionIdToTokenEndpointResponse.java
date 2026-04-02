package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddFAPIInteractionIdToTokenEndpointResponse extends AbstractCondition {

	@Override
	@PostEnvironment(required = "token_endpoint_response_headers")
	public Environment evaluate(Environment env) {
		String fapiInteractionId = env.getString("fapi_interaction_id");

		JsonObject headers = env.getObject("token_endpoint_response_headers");
		if (headers == null) {
			headers = new JsonObject();
		}

		if (!Strings.isNullOrEmpty(fapiInteractionId)) {
			headers.addProperty("x-fapi-interaction-id", fapiInteractionId);
			env.putObject("token_endpoint_response_headers", headers);
			logSuccess("Added FAPI Interaction ID to token endpoint response headers", headers);
		} else {
			log("FAPI Interaction ID not found, not added to headers");
		}

		return env;
	}
}
