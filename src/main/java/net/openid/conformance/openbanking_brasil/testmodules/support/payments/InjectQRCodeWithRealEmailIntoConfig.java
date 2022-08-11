package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.pixqrcode.PixQRCode;
import net.openid.conformance.testmodule.Environment;

public class InjectQRCodeWithRealEmailIntoConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource")
	public Environment evaluate(Environment env) {
		JsonObject consentPaymentDetails = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment.details");
		JsonObject paymentInitiation = (JsonObject) env.getElementFromObject("resource", "brazilPixPayment.data");

		PixQRCode qrCode = new PixQRCode();
		qrCode.useStandardConfig();
		qrCode.setTransactionAmount(env.getString("resource", "brazilPaymentConsent.data.payment.amount"));
		consentPaymentDetails.addProperty("qrCode", qrCode.toString());
		paymentInitiation.addProperty("qrCode", qrCode.toString());

		logSuccess("Added qr code to payment consent and payment initiation");

		return env;
	}
}
