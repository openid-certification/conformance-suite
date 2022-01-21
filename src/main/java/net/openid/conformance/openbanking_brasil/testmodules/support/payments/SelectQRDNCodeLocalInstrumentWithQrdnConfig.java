package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.testmodules.support.AbstractPaymentLocalInstrumentCondtion;

public class SelectQRDNCodeLocalInstrumentWithQrdnConfig extends AbstractPaymentLocalInstrumentCondtion {

	@Override
	protected String getLocalInstrument() {
		return "QRDN";
	}

	@Override
	protected JsonObject getPaymentConsentObject(JsonObject resourceConfig) {
		JsonElement brazilQrdnPaymentConsent = resourceConfig.get("brazilQrdnPaymentConsent");
		if (brazilQrdnPaymentConsent.isJsonObject()) {
			return brazilQrdnPaymentConsent.getAsJsonObject();
		} else {
			String massage = "Test cannot continue because brazilQrdnPaymentConsent cannot be " +
				"parsed. -> Show View Button -> Display brazilQrdnPaymentConsent right below";
			throw error(massage, args("brazilQrdnPaymentConsent", brazilQrdnPaymentConsent.toString()));
		}
	}
}
