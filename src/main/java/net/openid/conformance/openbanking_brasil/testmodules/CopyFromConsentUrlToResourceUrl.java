package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CopyFromConsentUrlToResourceUrl extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String consentUrl = env.getString("config","resource.consentUrl");
		env.putString("config", "resource.resourceUrl", consentUrl);

		logSuccess(String.format("resourceUrl for %s set up", consentUrl), args("resourceUrl", consentUrl));
		return env;
	}
}
