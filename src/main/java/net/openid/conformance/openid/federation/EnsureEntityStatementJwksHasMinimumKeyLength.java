package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractEnsureMinimumKeyLength;
import net.openid.conformance.testmodule.Environment;

public class EnsureEntityStatementJwksHasMinimumKeyLength extends AbstractEnsureMinimumKeyLength {

	private static final int MINIMUM_KEY_LENGTH_RSA = 2048;

	private static final int MINIMUM_KEY_LENGTH_EC = 256;

	private static final String JWKS_KEY = "ec_jwks";

	@Override
	@PreEnvironment(required = JWKS_KEY)
	public Environment evaluate(Environment env) {
		return checkKeyLength(env, JWKS_KEY, MINIMUM_KEY_LENGTH_RSA, MINIMUM_KEY_LENGTH_EC);
	}
}
