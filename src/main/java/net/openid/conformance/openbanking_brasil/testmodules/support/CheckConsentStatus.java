package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckConsentStatus extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "consent_id")
	public Environment evaluate(Environment env) {
		@SuppressWarnings("unused")
		String consentId = env.getString("consent_id");
		return env;
	}
}
