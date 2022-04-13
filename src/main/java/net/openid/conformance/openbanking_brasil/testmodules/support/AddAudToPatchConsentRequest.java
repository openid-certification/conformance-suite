package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddAudToPatchConsentRequest extends AbstractCondition {
	@Override
	@PreEnvironment(
		required = "request_object_claims",
		strings = "consent_url"
	)
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		String consentUrlWithId = env.getString("consent_url");

		requestObjectClaims.addProperty("aud", consentUrlWithId);

		logSuccess("Added aud to request object claims", args("aud", consentUrlWithId));

		return env;
	}
}
