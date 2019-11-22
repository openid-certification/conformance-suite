package net.openid.conformance.condition.common;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckDistinctKeyIdValueInServerJWKs extends AbstractCheckDistinctKeyIdValueInJWKs {

	@Override
	@PreEnvironment(required = "server_jwks")
	public Environment evaluate(Environment env) {
		return checkDistinctKeyIdValueInJWKs(env, "server_jwks");
	}

}
