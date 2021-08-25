package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddAudAsPaymentConsentUriToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		String resourceEndpoint = env.getString("config", "resource.consentUrl");
		if (Strings.isNullOrEmpty(resourceEndpoint)) {
			throw error("consent url missing from configuration");
		}

		requestObjectClaims.addProperty("aud", resourceEndpoint);

		logSuccess("Added aud to request object claims", args("aud", resourceEndpoint));

		return env;
	}
}
