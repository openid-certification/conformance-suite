package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class SetPaymentAmountToKnownValueOnConsent extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment");

		obj.addProperty("amount", "100.00");
		obj.addProperty("amount", "100.00");

		logSuccess("Added payment amount of 100.00 to payment consent");

		return env;
	}
}
