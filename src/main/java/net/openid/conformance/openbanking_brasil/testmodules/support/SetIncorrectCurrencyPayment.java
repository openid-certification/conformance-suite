package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class SetIncorrectCurrencyPayment extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject resource = env.getObject("resource");
		JsonObject consentRequest = resource.getAsJsonObject("brazilPaymentConsent");
		JsonObject paymentRequest = resource.getAsJsonObject("brazilPixPayment");

		String consentCurrency = OIDFJSON.getString(consentRequest
			.getAsJsonObject("data")
			.getAsJsonObject("payment")
			.get("currency"));

		if(consentCurrency == null){
			logFailure("Consent does not have an associated currency");
			return env;
		}

		env.putString(
			"previous_currency",
			OIDFJSON.getString(
				paymentRequest.getAsJsonObject("data").getAsJsonObject("payment").get("currency")
			)
		);
		log(env.getString("previous_currency"));

		String wrongCurrency = consentCurrency.equalsIgnoreCase("BRL") ? "ZAR" : "BRL";

		paymentRequest
			.getAsJsonObject("data")
			.getAsJsonObject("payment")
			.addProperty("currency", wrongCurrency);

		logSuccess("Successfully set the currency type of the payment request to differ from the consent request", paymentRequest);
		return env;
	}
}
