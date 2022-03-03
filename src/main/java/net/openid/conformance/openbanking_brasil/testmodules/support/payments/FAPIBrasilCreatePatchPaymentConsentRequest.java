package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrasilCreatePatchPaymentConsentRequest extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement patchConsent = env.getElementFromObject("resource","brazilPatchPaymentConsent");
		if (patchConsent == null){
			throw error("As 'patch' is included in the 'scope' within the test configuration, a payment consent request JSON object must also be provided in the test configuration.");
		}

		JsonObject patchRequestObject = patchConsent.getAsJsonObject();
		validate(patchRequestObject);
		env.putObject("consent_endpoint_request", patchRequestObject);

		logSuccess(args("consent_endpoint_request", patchRequestObject));

		return env;
	}

	private void validate(JsonObject patchConfig) {
		JsonElement element = validate("data", patchConfig);
		validate("status", element.getAsJsonObject());
		validate("revocation", element.getAsJsonObject());
	}

	private JsonElement validate(String element, JsonObject object) {
		if(!object.has(element)) {
			throw error("Patch Consent object must have " + element + " field");
		}
		return object.get(element);
	}
}

