package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.testmodules.support.AbstractPaymentLocalInstrumentCondtion;

public class SelectQRDNCodeLocalInstrumentWithQrdnConfig extends AbstractPaymentLocalInstrumentCondtion {

	@Override
	protected String getLocalInstrument() {
		return "QRDN";
	}

	@Override
	protected JsonObject getPaymentConsentObject(JsonObject resourceConfig) {
		return resourceConfig.getAsJsonObject("brazilQrdnPaymentConsent");
	}

}
