package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class SetProxyToRealEmailAddressOnPaymentConsent extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment.details");
		obj.addProperty("proxy", "cliente-000001@pix.bcb.gov.br");

		logSuccess("Added correct email address as proxy to payment consent");

		return env;
	}

}
