package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CdrEnsureTokenRequestInteractionIdMatchesParRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		String tokenRequestId = env.getString("incoming_request", "headers.x-fapi-interaction-id");
		String parRequestId = env.getString("par_request_fapi_interaction_id");

		if (Strings.isNullOrEmpty(parRequestId)) {
			logSuccess("The PAR request did not contain an x-fapi-interaction-id, so there is nothing to compare the token request header to");
			return env;
		}

		if (parRequestId.equals(tokenRequestId)) {
			logSuccess("The token request reuses the x-fapi-interaction-id from the PAR request", args("x-fapi-interaction-id", tokenRequestId));
			return env;
		}

		throw error("Data Recipients SHOULD reuse the same x-fapi-interaction-id value across the PAR request and the token endpoint request that follows the authorisation response",
			args("par_request_x_fapi_interaction_id", parRequestId, "token_request_x_fapi_interaction_id", tokenRequestId));
	}

}
