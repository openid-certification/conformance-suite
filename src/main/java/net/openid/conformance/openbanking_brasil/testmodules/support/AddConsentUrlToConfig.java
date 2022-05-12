package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddConsentUrlToConfig extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config" )
	public Environment evaluate(Environment env) {

		String consentURL = "https://matls-api.mockbank.poc.raidiam.io/open-banking/consents/v1/consents";
		env.putString("config", "resource.consentUrl", consentURL);
		return env;
	}
}
