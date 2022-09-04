package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class CreateFapiInteractionIdIfNeeded extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "fapi_interaction_id")
	public Environment evaluate(Environment env) {
		String fapiInteractionId = env.getString("fapi_interaction_id");

		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			fapiInteractionId = UUID.randomUUID().toString();

			logSuccess("Created new FAPI interaction ID", args("fapi_interaction_id", fapiInteractionId));

			env.putString("fapi_interaction_id", fapiInteractionId);

		} else {
			// if there's an existing one we just leave it there
			log("Found existing FAPI interaction ID",
				args("fapi_interaction_id", fapiInteractionId, "result", ConditionResult.INFO));
		}

		return env;

	}

}
