package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class CheckForFAPIInteractionIdInPARResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "pushed_authorization_endpoint_response_headers")
	public Environment evaluate(Environment env) {

		String interactionIdStr = env.getString("pushed_authorization_endpoint_response_headers", "x-fapi-interaction-id");

		if (Strings.isNullOrEmpty(interactionIdStr)) {
			throw error("x-fapi-interaction-id not found in pushed authorization endpoint response headers");
		}

		try {
			@SuppressWarnings("unused")
			UUID interactionId = UUID.fromString(interactionIdStr);
		} catch (IllegalArgumentException e) {
			throw error("Invalid x-fapi-interaction-id - not a UUID", args("interaction_id", interactionIdStr));
		}

		logSuccess("Found x-fapi-interaction-id", args("interaction_id", interactionIdStr));

		return env;
	}

}
