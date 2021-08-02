package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilCreatePaymentConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config" )
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement paymentConsent = env.getElementFromObject("resource", "brazilPaymentConsent");
		if(paymentConsent == null) {
			throw error("As 'payments' is included in the 'scope' within the test configuration, a payment consent request JSON object must also be provided in the test configuration.");
		}
		JsonObject paymentRequestObject = paymentConsent.getAsJsonObject();
		validate(paymentRequestObject);
		env.putObject("consent_endpoint_request", paymentRequestObject);

		logSuccess(args("consent_endpoint_request", paymentConsent));
		return env;
	}

	private void validate(JsonObject consentConfig) {
		JsonElement element = validate("data", consentConfig);
		validate("loggedUser", element.getAsJsonObject());
		validate("creditor", element.getAsJsonObject());
		validate("payment", element.getAsJsonObject());
	}

	private JsonElement validate(String element, JsonObject object) {
		if(!object.has(element)) {
			throw error("Consent object must have " + element + " field");
		}
		return object.get(element);
	}
}
