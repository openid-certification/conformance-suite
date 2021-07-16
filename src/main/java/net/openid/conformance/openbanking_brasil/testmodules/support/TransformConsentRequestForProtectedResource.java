package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class TransformConsentRequestForProtectedResource extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String consentUrl = env.getString("consent_url");
		env.putString("protected_resource_url", consentUrl);
		log("Consent url prepared for call with client credentials", args("url", consentUrl));
		return env;
	}
}
