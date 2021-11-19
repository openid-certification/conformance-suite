package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class InjectQRCodeWithWrongAmountIntoConfig extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment.details");

		obj.addProperty("qrCode", QrCodeKeys.QRES_WRONG_AMOUNT);

		logSuccess("Added qr code to payment consent with a payment of 123.45");

		return env;
	}
}
