package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateSHash extends AbstractValidateHash {

	@Override
	@PreEnvironment(strings = "state", required = "s_hash")
	public Environment evaluate(Environment env) {
		return super.validateHash(env, "s_hash", "s_hash");
	}

}
