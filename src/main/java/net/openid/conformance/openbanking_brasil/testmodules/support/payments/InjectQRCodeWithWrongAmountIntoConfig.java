package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.pixqrcode.PixQRCode;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Random;

public class InjectQRCodeWithWrongAmountIntoConfig extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject consentPayment = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment");
		String givenAmount = OIDFJSON.getString(consentPayment.get("amount"));

		JsonObject paymentInitiation = (JsonObject) env.getElementFromObject("resource", "brazilPixPayment.data");
		JsonObject consentPaymentDetails = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment.details");

		//Build the QRes
		PixQRCode qrCode = new PixQRCode();
		qrCode.useStandardConfig();

		Random r = new Random();
		String amount = "100.00";
		do{
			float random = r.nextFloat() * 100;
			amount = String.format("%.02f", random);
		}while(givenAmount.equals(amount));
		qrCode.setTransactionAmount(amount);

		consentPaymentDetails.addProperty("qrCode", qrCode.toString());
		paymentInitiation.addProperty("qrCode", qrCode.toString());

		logSuccess(String.format("Added new QRes to payment consent and payment initiation with amount %s BRL", amount), args("QRes", qrCode.toString()));


		return env;
	}
}
