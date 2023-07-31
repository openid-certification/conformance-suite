package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class ValidateFAPIInteractionIdInResourceRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String interactionIdStr = env.getString("fapi_interaction_id");

		if (Strings.isNullOrEmpty(interactionIdStr)) {
			log("x-fapi-interaction-id not found in resource endpoint request headers");
			return env;
		}

		try {
			@SuppressWarnings("unused")
			UUID interactionId = UUID.fromString(interactionIdStr);
		} catch (IllegalArgumentException e) {
			throw error("Invalid x-fapi-interaction-id in response request headers- not a UUID", args("interaction_id", interactionIdStr));
		}

		logSuccess("x-fapi-interaction-id in resource request headers is a valid UUID", args("interaction_id", interactionIdStr));

		return env;
	}

}
