package io.fintechlabs.testframework.condition.client;

import org.apache.commons.lang3.RandomStringUtils;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

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
