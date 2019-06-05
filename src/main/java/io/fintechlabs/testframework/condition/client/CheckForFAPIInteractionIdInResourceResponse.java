package io.fintechlabs.testframework.condition.client;

import java.util.UUID;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForFAPIInteractionIdInResourceResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_headers")
	public Environment evaluate(Environment env) {

		String interactionIdStr = env.getString("resource_endpoint_response_headers", "x-fapi-interaction-id");

		if (Strings.isNullOrEmpty(interactionIdStr)) {
			throw error("x-fapi-interaction-id not found in resource endpoint response headers");
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
