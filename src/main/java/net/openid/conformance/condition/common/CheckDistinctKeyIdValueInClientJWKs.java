package net.openid.conformance.condition.common;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckDistinctKeyIdValueInClientJWKs extends AbstractCheckDistinctKeyIdValueInJWKs {

	@Override
	@PreEnvironment(required = "client_jwks")
	public Environment evaluate(Environment env) {
		return checkDistinctKeyIdValueInJWKs(env, "client_jwks");
	}

}
