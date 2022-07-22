package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SwitchToSecondaryConsentUrl extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		String consentUrl = env.getString("config", "resource.consentUrl");
		String secondaryConsentUrl = env.getString("config", "resource.consentUrl2");
		env.putString("config", "resource.consentUrl", secondaryConsentUrl);
		logSuccess("Switched to the secondaryConsentUrl", args("secondaryConsentUrl", secondaryConsentUrl, "consentUrl", consentUrl));
		return env;
	}
}
