package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractCheckDistinctKeyIdValueInJWKs;
import net.openid.conformance.testmodule.Environment;

public class CheckDistinctKeyIdValueInEntityStatementJWKs extends AbstractCheckDistinctKeyIdValueInJWKs {

	@Override
	@PreEnvironment(required = "ec_jwks")
	public Environment evaluate(Environment env) {
		return checkDistinctKeyIdValueInJWKs(env, "ec_jwks");
	}
}
