package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPI2FinalEnsureMinimumClientKeyLength extends AbstractEnsureMinimumKeyLength {

	private static final int MINIMUM_KEY_LENGTH_RSA = 2048;

	private static final int MINIMUM_KEY_LENGTH_EC = 224;

	private static final String JWKS_KEY = "client_jwks";

	@Override
	@PreEnvironment(required = JWKS_KEY)
	public Environment evaluate(Environment env) {
		return checkKeyLength(env, JWKS_KEY, MINIMUM_KEY_LENGTH_RSA, MINIMUM_KEY_LENGTH_EC);
	}

}
