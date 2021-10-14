package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidatePaymentAndConsentHaveSameProperties extends AbstractCondition {


	@Override
	public Environment evaluate(Environment env) {
		JsonObject resource = env.getObject("resource");
		JsonObject consentRequest = resource
										.getAsJsonObject("brazilPaymentConsent")
										.getAsJsonObject("data")
										.getAsJsonObject("payment");
		JsonObject paymentRequest = resource
										.getAsJsonObject("brazilPixPayment")
										.getAsJsonObject("data")
										.getAsJsonObject("payment");

		String paymentAmount = OIDFJSON.getString(paymentRequest.get("amount"));
		String paymentCurrency = OIDFJSON.getString(paymentRequest.get("currency"));
		String consentAmount = OIDFJSON.getString(consentRequest.get("amount"));
		String consentCurrency = OIDFJSON.getString(consentRequest.get("currency"));

		if (paymentAmount.equalsIgnoreCase(consentAmount)) {
			logSuccess("Payment and consent request have matching amounts");
		} else {
			logFailure("Payment and consent do not have matching amounts");
			return env;
		}

		if (paymentCurrency.equalsIgnoreCase(consentCurrency)) {
			logSuccess("Payment and consent request have matching amounts");
		} else {
			logFailure("Payment and consent do not have matching amounts");
			return env;
		}

		logSuccess("Both payment and consent request match correctly.");

		return env;
	}
}
