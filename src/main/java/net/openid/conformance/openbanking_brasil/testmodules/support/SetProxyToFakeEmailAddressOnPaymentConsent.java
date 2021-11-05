package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class SetProxyToFakeEmailAddressOnPaymentConsent extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment.details");
		obj.addProperty("proxy", "fakeperson@example.com");

		logSuccess("Added non-existent email address as proxy to payment consent");

		return env;
	}

}
