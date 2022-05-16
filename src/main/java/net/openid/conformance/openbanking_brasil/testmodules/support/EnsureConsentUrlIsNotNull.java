package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureConsentUrlIsNotNull extends AbstractCondition {
	@PreEnvironment(required = "config")
	@Override
	public Environment evaluate (Environment env){
		String consentUrl = env.getString("config", "resource.consentUrl");
		if(Strings.isNullOrEmpty(consentUrl)) {
			throw error("consentUrl is missing. field consentUrl must be specified in the test configuration.");
		}
		logSuccess("consentUrl successfully found", args("consentUrl", consentUrl));
		return env;
	}
}
