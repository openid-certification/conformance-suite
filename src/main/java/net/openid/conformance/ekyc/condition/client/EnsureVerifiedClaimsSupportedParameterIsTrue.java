package net.openid.conformance.ekyc.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractValidateJsonBoolean;
import net.openid.conformance.testmodule.Environment;

public class EnsureVerifiedClaimsSupportedParameterIsTrue extends AbstractValidateJsonBoolean {
	private static final String environmentVariable = "verified_claims_supported";
	private static final boolean requiredValue = true;
	private static final boolean defaultValue = false;

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, defaultValue, requiredValue);

	}

}
