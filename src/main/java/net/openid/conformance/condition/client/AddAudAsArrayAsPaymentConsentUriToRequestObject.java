package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddAudAsArrayAsPaymentConsentUriToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		String resourceEndpoint = env.getString("config", "resource.consentUrl");
		if (Strings.isNullOrEmpty(resourceEndpoint)) {
			throw error("consent url missing from configuration");
		}

		JsonArray audArray = new JsonArray();
		audArray.add(resourceEndpoint);

		requestObjectClaims.add("aud", audArray);

		logSuccess("Added aud to request object claims", args("aud", audArray));

		return env;
	}
}
