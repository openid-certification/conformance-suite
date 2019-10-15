package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;

public class CreateRandomNonceValue extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "nonce")
	public Environment evaluate(Environment env) {

		Integer nonceLength = env.getInteger("requested_nonce_length");

		if (nonceLength == null) {
			nonceLength = 10; // default to a nonce of length 10
		}

		String nonce = RandomStringUtils.randomAlphanumeric(nonceLength);
		env.putString("nonce", nonce);

		log("Created nonce value", args("nonce", nonce, "requested_nonce_length", nonceLength));

		return env;
	}

}
