package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class GenerateCredentialNonce extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String nonce = RandomStringUtils.secure().nextAlphanumeric(32);

		env.putString("credential_issuer_nonce", nonce);

		logSuccess("Created credential nonce", args("nonce", nonce));

		return env;
	}
}
