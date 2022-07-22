package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureSecondaryConsentUrlIsNotNull extends AbstractCondition {
	@PreEnvironment(required = "config")
	@Override
	public Environment evaluate (Environment env){
		String secondaryConsentUrl = env.getString("config", "resource.consentUrl2");
		if(Strings.isNullOrEmpty(secondaryConsentUrl)) {
			throw error("SecondaryConsentUrl is missing. field SecondaryConsentUrl must be specified in the test configuration.");
		}
		logSuccess("SecondaryConsentUrl successfully found", args("SecondaryConsentUrl", secondaryConsentUrl));
		return env;
	}
}
