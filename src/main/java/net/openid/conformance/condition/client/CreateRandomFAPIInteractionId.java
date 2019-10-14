package net.openid.conformance.condition.client;

import java.util.UUID;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateRandomFAPIInteractionId extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "fapi_interaction_id")
	public Environment evaluate(Environment env) {

		UUID interactionId = UUID.randomUUID();
		env.putString("fapi_interaction_id", interactionId.toString());

		log("Created interaction ID", args("fapi_interaction_id", interactionId.toString()));

		return env;
	}

}
