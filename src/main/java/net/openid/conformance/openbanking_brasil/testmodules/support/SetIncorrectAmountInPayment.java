package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class SetIncorrectAmountInPayment extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject resource = env.getObject("resource");
		JsonObject consentRequest = resource.getAsJsonObject("brazilPaymentConsent");
		JsonObject paymentRequest = resource.getAsJsonObject("brazilPixPayment");

		String consentAmount = OIDFJSON.getString(consentRequest
			.getAsJsonObject("data")
			.getAsJsonObject("payment")
			.get("amount"));

		double newAmount;
		try {
			newAmount = Double.parseDouble(consentAmount) + 100;
		} catch(Exception e){
			logFailure(String.format(
				"There was an error parsing an integer from the amount. This field may have been left empty." +
					"\nDebug message: {} , amount: {}",
				e.getMessage(), consentAmount
			));
			return env;
		}

		paymentRequest
			.getAsJsonObject("data")
			.getAsJsonObject("payment")
			.addProperty("amount", Double.toString(newAmount));

		logSuccess("Successfully set the amount in the payment request to differ from the consent", paymentRequest);
		return env;
	}
}
