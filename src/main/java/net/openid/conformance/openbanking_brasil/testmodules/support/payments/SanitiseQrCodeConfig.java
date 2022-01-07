package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;

public class SanitiseQrCodeConfig extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		Set<String> qrInstruments = Set.of("QRDN", "QRES");
		String localInstrument = env.getString("config", "resource.brazilPaymentConsent.data.payment.details.localInstrument");
		if(qrInstruments.contains(localInstrument)) {
			logSuccess("Local Instrument is QRES or QRDN - leaving alone");
			return env;
		}
		JsonObject consentConfig = (JsonObject) env.getElementFromObject("config", "resource.brazilPaymentConsent.data.payment.details");
		JsonObject paymentConfig = (JsonObject) env.getElementFromObject("config", "resource.brazilPixPayment.data");
		sanitise(consentConfig, "consent");
		sanitise(paymentConfig, "payment");
		return env;
	}

	private void sanitise(JsonObject config, String name) {
		if(config != null) {
			config.remove("qrCode");
			logSuccess("Removed qrCode from element", Map.of("element", name));
		} else {
			logSuccess("Element not present - leaving alone", Map.of("element", name));
		}
	}
}
