package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.pixqrcode.PixQRCode;
import net.openid.conformance.testmodule.Environment;

public class InjectQRCodeWithRealPhoneNumberIntoConfig extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		//Build the QRes
		PixQRCode qrCode = new PixQRCode();
		qrCode.useStandardConfig();

		//Add proxy as the phone number
		qrCode.setProxy(DictHomologKeys.PROXY_PHONE_NUMBER);

		JsonObject consentPaymentDetails = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment.details");
		JsonObject paymentInitiation = (JsonObject) env.getElementFromObject("resource", "brazilPixPayment.data");
		consentPaymentDetails.addProperty("qrCode", qrCode.toString());
		paymentInitiation.addProperty("qrCode", qrCode.toString());

		logSuccess("Added new QRes to payment consent and payment initiation", args("QRes", qrCode.toString()));

		return env;
	}
}
