package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class CreateRandomNonceValue extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "nonce")
	public Environment evaluate(Environment env) {

		Integer nonceLength = env.getInteger("requested_nonce_length");

		if (nonceLength == null) {
			nonceLength = 10; // default to a nonce of length 10
		}

		String nonce;
		if (nonceLength > 10) {
			// Check that any url safe character can be used when using a longer nonce value
			nonce = RandomStringUtils.secure().nextAlphanumeric(nonceLength-4) + "-._~";
		} else {
			// this is a more restricted character set than https://tools.ietf.org/html/rfc6749#appendix-A.5 which
			// allows 0x20-0x7E; presumably an attempt to avoid potentially problem prone characters
			nonce = RandomStringUtils.secure().nextAlphanumeric(nonceLength);
		}
		env.putString("nonce", nonce);

		log("Created nonce value", args("nonce", nonce, "requested_nonce_length", nonceLength));

		return env;
	}

}
