package net.openid.conformance.ekyc.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateVerifiedClaimsInIdTokenAgainstOPMetadata extends AbstractValidateVerifiedClaimsResponseAgainstOPMetadata {
	@Override
	@PreEnvironment(required = {"verified_claims_response", "server"})
	public Environment evaluate(Environment env) {
		return validate(env, "id_token");
	}

}
