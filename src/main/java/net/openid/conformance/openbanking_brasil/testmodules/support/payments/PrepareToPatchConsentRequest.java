package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareToPatchConsentRequest extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config", strings = "consent_id")
	@PostEnvironment(strings = {"consent_url"})
	public Environment evaluate(Environment env) {
		String resourceUrl = env.getString("config", "resource.consentUrl");
		String consentId = env.getString("consent_id");
		resourceUrl = String.format("%s/%s", resourceUrl, consentId);
		env.putString("consent_url", resourceUrl);
		logSuccess("Ready to PATCH consent",args("resourceUrl",resourceUrl));
		return env;
	}
}
