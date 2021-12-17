package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class EnsureConsentStatusIsAwaitingAuthorisation extends AbstractCondition {

	private static final String EXPECTED_STATUS = "AWAITING_AUTHORISATION";

	@Override
	@PreEnvironment(required = "consent_endpoint_response")
	public Environment evaluate(Environment env) {
		String path = "data.status";

		String consentStatus = env.getString("consent_endpoint_response", path);
		if (Strings.isNullOrEmpty(consentStatus)) {
			throw error("Couldn't find " + path + " in the consent response",
				args("consent_endpoint_response", env.getObject("consent_endpoint_response")));
		}

		if(consentStatus.equals(EXPECTED_STATUS)) {
			logSuccess("Consent status is " + EXPECTED_STATUS);
			return env;
		}

		throw error("Consent was not in the " + EXPECTED_STATUS + " status", Map.of("status", consentStatus));


	}
}
