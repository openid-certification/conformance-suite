package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.common.base.Strings;
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
		String amount = env.getString("resource", "brazilPaymentConsent.data.payment.amount");
		if(Strings.isNullOrEmpty(amount)){
			throw error("Could not find amount in the payments consents resource");
		}
		qrCode.setTransactionAmount(amount);

		//Add proxy as the phone number
		qrCode.setProxy(DictHomologKeys.PROXY_PHONE_NUMBER);
		JsonObject consentPaymentDetails = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment.details");
		JsonObject paymentInitiation = (JsonObject) env.getElementFromObject("resource", "brazilPixPayment.data");
		consentPaymentDetails.addProperty("qrCode", qrCode.toString());
		paymentInitiation.addProperty("qrCode", qrCode.toString());

		logSuccess("Added new qrCode to payment consent and payment initiation ", args("qrCode", qrCode.toString()));

		return env;
	}
}
