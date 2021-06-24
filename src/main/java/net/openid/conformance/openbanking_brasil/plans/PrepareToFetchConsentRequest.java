package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class PrepareToFetchConsentRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String resourceUrl = env.getString("config", "resource.consentUrl");
		String consentId = env.getString("consentId");
		resourceUrl = String.format("%s/%s", resourceUrl, consentId);
		env.putString("consent_url", resourceUrl);
		env.putString("http_method", "GET");
		return env;
	}
}
