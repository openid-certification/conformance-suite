package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class CheckForFAPIInteractionIdInResourceRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String interactionIdStr = env.getString("fapi_interaction_id");

		if (Strings.isNullOrEmpty(interactionIdStr)) {
			throw error("x-fapi-interaction-id not found in resource endpoint request headers");
		}

		logSuccess("Found x-fapi-interaction-id", args("interaction_id", interactionIdStr));

		return env;
	}

}
