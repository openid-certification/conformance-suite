package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureConsentRejectAspspMaxDateReached extends AbstractConsentRejectionValidation {
	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		if (validateResponse(env.getString("resource_endpoint_response"),"CONSENT_MAX_DATE_REACHED","ASPSP")){
			throw error("Rejected object was not found.");
		}
		return env;
	}
}
