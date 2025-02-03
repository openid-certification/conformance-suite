package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class CreateRandomStateValue extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "state")
	public Environment evaluate(Environment env) {

		Integer stateLength = env.getInteger("requested_state_length");

		if (stateLength == null) {
			stateLength = 10; // default to a state of length 10
		}

		String state;
		if (stateLength > 10) {
			// as per https://gitlab.com/openid/conformance-suite/-/issues/1226 JWTs are commonly used for state
			// values - we hence check that any url safe character can be used when using a longer state value
			state = RandomStringUtils.secure().nextAlphanumeric(stateLength-4) + "-._~";
		} else {
			// this is a more restricted character set than https://tools.ietf.org/html/rfc6749#appendix-A.5 which
			// allows 0x20-0x7E; presumably an attempt to avoid potentially problem prone characters
			state = RandomStringUtils.secure().nextAlphanumeric(stateLength);
		}
		env.putString("state", state);

		log("Created state value", args("state", state, "requested_state_length", stateLength));

		return env;
	}

}
