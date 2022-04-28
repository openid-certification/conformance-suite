package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class InjectQRCodeWithWrongEmailAddressProxyIntoConfig extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject consentPaymentDetails = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment.details");
		JsonObject paymentInitiation = (JsonObject) env.getElementFromObject("resource", "brazilPixPayment.data");

		consentPaymentDetails.addProperty("qrCode", QrCodeKeys.QRES_PHONE_NUMBER);
		paymentInitiation.addProperty("qrCode", QrCodeKeys.QRES_PHONE_NUMBER);

		logSuccess("Added qr code to payment consent and payment initiation");

		return env;
	}
}