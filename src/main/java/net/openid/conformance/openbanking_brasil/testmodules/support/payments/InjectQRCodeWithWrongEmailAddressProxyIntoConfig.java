package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.pixqrcode.PixQRCode;
import net.openid.conformance.testmodule.Environment;

import java.util.Random;

public class InjectQRCodeWithWrongEmailAddressProxyIntoConfig extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		//Build the QRes
		PixQRCode qrCode = new PixQRCode();
		qrCode.useStandardConfig();

		//Generate random amount
		Random r = new Random();
		float random = r.nextFloat() * 100;
		String amount = String.format("%.02f", random);
		qrCode.setTransactionAmount(amount);

		//Mismatched proxy
		qrCode.setProxy(DictHomologKeys.PROXY_PHONE_NUMBER);

		JsonObject obj = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment");
		obj.addProperty("amount", amount);

		obj = (JsonObject) env.getElementFromObject("resource", "brazilPixPayment.data.payment");
		obj.addProperty("amount", amount);


		JsonObject consentPaymentDetails = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment.details");
		JsonObject paymentInitiation = (JsonObject) env.getElementFromObject("resource", "brazilPixPayment.data");
		consentPaymentDetails.addProperty("qrCode", qrCode.toString());
		paymentInitiation.addProperty("qrCode", qrCode.toString());

		logSuccess(String.format("Added new QRes to payment consent and payment initiation with amount %s BRL", amount), args("QRes", qrCode.toString()));
		return env;
	}
}
