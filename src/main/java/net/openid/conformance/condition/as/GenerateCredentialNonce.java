package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class GenerateCredentialNonce extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String nonce = RandomStringUtils.secure().nextAlphanumeric(32);

		// Only the most recently issued c_nonce is retained; a batch credential request is
		// expected to reuse this single value across all of its proofs. OID4VCI is silent on
		// whether batch proofs must share one c_nonce - see
		// https://github.com/openid/OpenID4VCI/issues/774
		env.putString("credential_issuer_nonce", nonce);

		logSuccess("Created credential nonce", args("nonce", nonce));

		return env;
	}
}
