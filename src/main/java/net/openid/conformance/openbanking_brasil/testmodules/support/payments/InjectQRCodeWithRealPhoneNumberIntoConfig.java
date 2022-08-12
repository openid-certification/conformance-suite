package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.pixqrcode.PixQRCode;
import net.openid.conformance.testmodule.Environment;

import java.util.Random;

public class InjectQRCodeWithRealPhoneNumberIntoConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource")
	public Environment evaluate(Environment env) {

		//Build the QRes
		PixQRCode qrCode = new PixQRCode();
		qrCode.useStandardConfig();


		Random random = new Random();
		int amountFractionalPart = random.ints(0, 99)
			.findFirst().orElseThrow(() -> error("Could not generate random fractional part"));

		// Amount has to be always within the following range 1333.00 - 1333.99
		String amount = String.format("1333.%02d", amountFractionalPart);

		qrCode.setTransactionAmount(amount);

		//Add proxy as the phone number
		qrCode.setProxy(DictHomologKeys.PROXY_PHONE_NUMBER);
		JsonObject consentPaymentDetails = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment.details");
		JsonObject paymentInitiation = (JsonObject) env.getElementFromObject("resource", "brazilPixPayment.data");
		consentPaymentDetails.addProperty("qrCode", qrCode.toString());
		paymentInitiation.addProperty("qrCode", qrCode.toString());

		env.putString("resource", "brazilPaymentConsent.data.payment.amount", amount);
		env.putString("resource", "brazilPixPayment.data.payment.amount", amount);


		logSuccess("Added new qrCode to payment consent and payment initiation with random amount",
			args("qrCode", qrCode.toString(), "amount", amount));

		return env;
	}
}
