package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class SetPaymentAmountToKnownValueOnPaymentInitiation extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = (JsonObject) env.getElementFromObject("resource", "brazilPixPayment.data.payment");

		obj.addProperty("amount", "100.00");

		logSuccess("Added payment amount of 100.00 to payment");

		return env;
	}
}
