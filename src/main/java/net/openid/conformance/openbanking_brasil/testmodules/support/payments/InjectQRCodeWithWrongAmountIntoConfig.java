package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.pixqrcode.PixQRCode;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.StringUtils;
import java.util.Locale;
import java.util.Random;

public class InjectQRCodeWithWrongAmountIntoConfig extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		//Retrieves creditor information from PaymentConsent
		JsonObject creditor = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.creditor");
		String creditorName = creditor.get("name").getAsString();

		JsonObject consentPayment = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment");
		String currency = consentPayment.get("currency").getAsString();
		String givenAmount = consentPayment.get("amount").getAsString();


		JsonObject paymentInitiation = (JsonObject) env.getElementFromObject("resource", "brazilPixPayment.data");
		JsonObject consentPaymentDetails = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment.details");
		String proxy = consentPaymentDetails.get("proxy").getAsString();


		//Build the QRes
		PixQRCode qrCode = new PixQRCode();
		qrCode.setPayloadFormatIndicator("01");
		qrCode.setProxy(proxy);
		qrCode.setMerchantCategoryCode("0000");

		currency = currency.equals("BRL") ? "986" : StringUtils.EMPTY;
		qrCode.setTransactionCurrency(currency);

		//Generates a random amount different from the one provided in the config
		Random r = new Random();
		String amount = "100.00";
		do{
			float random = r.nextFloat() * 100;
			amount = String.format("%.02f", random);
		}while(givenAmount.equals(amount));

		qrCode.setTransactionAmount(amount);

		qrCode.setCountryCode("BR");
		qrCode.setMerchantName(creditorName.toUpperCase(Locale.ROOT));
		qrCode.setMerchantCity("BELO HORIZONTE");
		qrCode.setAdditionalField("03***");


		consentPaymentDetails.addProperty("qrCode", qrCode.toString());
		paymentInitiation.addProperty("qrCode", qrCode.toString());

		logSuccess(String.format("Added new QRes to payment consent and payment initiation with amount %s , %s", amount, qrCode.toString()));

		return env;
	}
}
