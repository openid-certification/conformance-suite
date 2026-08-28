package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * The KSA Open Finance security profile caps the access token expiry at 10 minutes, so the
 * emulated authorization server must stay within that when driving the KSA RP tests.
 */
public class KsaGenerateAccessTokenExpiration extends AbstractCondition {

	@Override
	@PostEnvironment(strings = { "access_token_expiration"})
	public Environment evaluate(Environment env) {
		env.putString("access_token_expiration", "500");
		log("Set access_token_expiration to 500");
		return env;
	}

}
