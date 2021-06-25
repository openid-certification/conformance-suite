package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareToFetchConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config", strings = "consent_id")
	@PostEnvironment(strings = {"consent_url", "http_method"})
	public Environment evaluate(Environment env) {
		String resourceUrl = env.getString("config", "resource.consentUrl");
		String consentId = env.getString("consent_id");
		resourceUrl = String.format("%s/%s", resourceUrl, consentId);
		env.putString("consent_url", resourceUrl);
		env.putString("http_method", "GET");
		log("Fetching consent from consent API", args("consentId", consentId,"url", resourceUrl));
		return env;
	}

}
