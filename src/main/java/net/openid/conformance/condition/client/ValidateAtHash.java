package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateAtHash extends AbstractValidateHash {

	@Override
	@PreEnvironment(required = { "access_token", "at_hash" } )
	public Environment evaluate(Environment env) {
		return super.validateHash(env,"at_hash","at_hash");
	}


}
