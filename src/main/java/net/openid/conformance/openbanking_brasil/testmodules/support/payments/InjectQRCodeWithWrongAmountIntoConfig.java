package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.emv.qrcode.decoder.mpm.DecoderMpm;
import com.emv.qrcode.model.mpm.MerchantPresentedMode;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class InjectQRCodeWithWrongAmountIntoConfig extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data.payment.details");

		MerchantPresentedMode code = DecoderMpm.decode(QrCodeKeys.QRES_EMAIL, MerchantPresentedMode.class);
		code.setTransactionAmount("123.00");
		obj.addProperty("qrCode", code.toString());

		logSuccess("Added qr code to payment consent with a payment of 123.00");

		return env;
	}
}
