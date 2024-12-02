package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.text.RandomStringGenerator;

import java.security.SecureRandom;

public class CreateRandomCodeVerifier extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "code_verifier")
	public Environment evaluate(Environment env) {

		// https://tools.ietf.org/html/rfc7636#section-4.1
		//
		// code_verifier = high-entropy cryptographic random STRING using the
		// unreserved characters [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"
		// from Section 2.3 of [RFC3986], with a minimum length of 43 characters
		// and a maximum length of 128 characters.

		char [][] pairs = {
			{'A','Z'},
			{'a','z'},
			{'0','9'},
			{'-','-'},
			{'.','.'},
			{'_','_'},
			{'~','~'},
		};
		// In a real client base64url of SecureRandom would be fine/simpler; we don't
		// use it here as it would not include the . and ~ characters and we want to
		// test the full range of permitted characters
		final SecureRandom rand = new SecureRandom();
		RandomStringGenerator generator = new RandomStringGenerator.Builder()
			.usingRandom(rand::nextInt)
			.withinRange(pairs).get();
		// test maximum permitted length
		String verifier = generator.generate(128);

		env.putString("code_verifier", verifier);

		log("Created code_verifier value", args("code_verifier", verifier));

		return env;
	}
}
