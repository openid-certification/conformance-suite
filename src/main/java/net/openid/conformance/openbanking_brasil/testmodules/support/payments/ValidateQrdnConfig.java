package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.emv.qrcode.decoder.mpm.DecoderMpm;
import com.emv.qrcode.model.mpm.MerchantPresentedMode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class ValidateQrdnConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config" )
	public Environment evaluate(Environment env) {
		JsonObject paymentDetails = (JsonObject) env.getElementFromObject("resource", "brazilQrdnPaymentConsent.data.payment.details");
		if(paymentDetails == null) {
			throw error("QRDN consent config is missing or incomplete");
		}
		JsonElement qrCodeElement = paymentDetails.get("qrCode");
		if(qrCodeElement == null) {
			throw error("QRDN consent config does not contain a qr code");
		}
		JsonElement brazilQrdnCnpj = env.getElementFromObject("resource", "brazilQrdnCnpj");
		if(brazilQrdnCnpj == null) {
			throw error("QRDN consent config does not contain an initiators CNPJ");
		}

		JsonElement remittanceAdvice = env.getElementFromObject("resource", "brazilQrdnRemittance");
		if(remittanceAdvice == null) {
			throw error("QRDN config does not contain a remittance advice");
		}

		String qrCode = OIDFJSON.getString(qrCodeElement);
		try {
			DecoderMpm.decode(qrCode, MerchantPresentedMode.class);
		} catch (Exception e) {
			throw error("QRDN consent config QR code is not an EMV QR code", Map.of("qrCode", qrCode, "exception", e.getMessage()));
		}
		logSuccess("QRDN consent config QR code is a valid EMV QR code", Map.of("qrCode", qrCode));
		return env;
	}
}
