package net.openid.conformance.openbanking_brasil.testmodules.account;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BuildAccountsConfigResourceUrlFromConsentUrl extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		String consentUrl = env.getString("config","resource.consentUrl");
		String resourceReplacement = "accounts";

		String validatorRegex = "^(https://)(.*?)(consents/v[0-9]/consents)";
		if(!consentUrl.matches(validatorRegex)) {
			throw error("consentUrl is not valid, please ensure that url matches " + validatorRegex, args("consentUrl", consentUrl));
		}

		String resourceUrl = consentUrl.replaceAll("consents", resourceReplacement);
		env.putString("config", "resource.resourceUrl", resourceUrl);

		logSuccess(String.format("resourceUrl for %s set up", resourceReplacement), args("resourceUrl", resourceUrl));
		return env;
	}
}
