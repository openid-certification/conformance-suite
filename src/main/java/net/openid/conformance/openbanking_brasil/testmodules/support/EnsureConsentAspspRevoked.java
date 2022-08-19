package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureConsentAspspRevoked extends AbstractConsentRejectionValidation {
	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		if (validateResponse(env.getString("resource_endpoint_response"),"CUSTOMER_MANUALLY_REVOKED","USER")){
			env.putBoolean("code_returned", true);
		}
		env.putBoolean("code_returned", false);
		return env;
	}
}
