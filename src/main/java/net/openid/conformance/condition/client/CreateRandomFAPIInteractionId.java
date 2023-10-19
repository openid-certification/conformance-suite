package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class CreateRandomFAPIInteractionId extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "fapi_interaction_id")
	public Environment evaluate(Environment env) {

		String  uuid = UUID.randomUUID().toString();
		String  interactionId = "";
		boolean toUpper = false;

		// Ensure the hex characters, [a-f], in the UUID are a mix of upper/lower case.
		for (int i=0; i<uuid.length(); i++) {
			char c = uuid.charAt(i);

			if (Character.isLowerCase(c)) {
				if (toUpper) {
					c = Character.toUpperCase(c);
				}

				toUpper = !toUpper;
			}

			interactionId += c;
		}

		env.putString("fapi_interaction_id", interactionId);

		log("Created interaction ID", args("fapi_interaction_id", interactionId));

		return env;
	}

}
