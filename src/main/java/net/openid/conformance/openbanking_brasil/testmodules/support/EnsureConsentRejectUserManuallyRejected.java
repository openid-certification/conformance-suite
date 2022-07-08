package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureConsentRejectUserManuallyRejected extends AbstractConsentRejectionValidation {
	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {

		if (!validateResponse(env.getString("resource_endpoint_response"),"COSTUMER_MANUALLY_REJECTED","USER")){
			throw error("Rejection object was not found.");
		}

		return env;
	}
}
