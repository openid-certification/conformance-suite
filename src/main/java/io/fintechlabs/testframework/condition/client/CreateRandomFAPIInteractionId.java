package io.fintechlabs.testframework.condition.client;

import java.util.UUID;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

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
