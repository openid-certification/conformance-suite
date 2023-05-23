package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckDiscEndpointAuthSignAlgValuesIsJsonArray extends AbstractEnsureJsonArray {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env, "server", "authorization_signing_alg_values_supported", true, false);
	}
}
