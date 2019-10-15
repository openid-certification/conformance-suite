package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;

public class CreateRandomStateValue extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "state")
	public Environment evaluate(Environment env) {

		Integer stateLength = env.getInteger("requested_state_length");

		if (stateLength == null) {
			stateLength = 10; // default to a state of length 10
		}

		String state = RandomStringUtils.randomAlphanumeric(stateLength);
		env.putString("state", state);

		log("Created state value", args("state", state, "requested_state_length", stateLength));

		return env;
	}

}
