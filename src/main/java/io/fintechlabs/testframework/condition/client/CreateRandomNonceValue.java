package io.fintechlabs.testframework.condition.client;

import org.apache.commons.lang3.RandomStringUtils;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreateRandomNonceValue extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "nonce")
	public Environment evaluate(Environment env) {

		String nonce = RandomStringUtils.randomAlphanumeric(10);
		env.putString("nonce", nonce);

		log("Created nonce value", args("nonce", nonce));

		return env;

	}

}
