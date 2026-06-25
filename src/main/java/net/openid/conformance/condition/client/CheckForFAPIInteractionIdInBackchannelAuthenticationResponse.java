package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class CheckForFAPIInteractionIdInBackchannelAuthenticationResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response_headers")
	public Environment evaluate(Environment env) {

		String interactionId = env.getString("backchannel_authentication_endpoint_response_headers",
			"x-fapi-interaction-id");
		if (Strings.isNullOrEmpty(interactionId)) {
			throw error("x-fapi-interaction-id not found in backchannel authentication endpoint response headers");
		}

		try {
			UUID.fromString(interactionId);
		} catch (IllegalArgumentException e) {
			throw error("Invalid x-fapi-interaction-id - not a UUID", args("interaction_id", interactionId));
		}

		logSuccess("Found x-fapi-interaction-id", args("interaction_id", interactionId));
		return env;
	}
}
