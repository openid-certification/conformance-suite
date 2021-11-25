package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

public class SelectPaymentConsentWithQrdnCode extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config" )
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement paymentConsent = env.getElementFromObject("resource", "brazilQrdnPaymentConsent");
		if(paymentConsent == null) {
			throw error("As 'payments' is included in the 'scope' within the test configuration, a payment consent request JSON object with QRDN must also be provided in the test configuration.");
		}
		JsonObject paymentRequestObject = paymentConsent.getAsJsonObject();
		validate(paymentRequestObject);

		JsonObject paymentDetails = paymentRequestObject.get("data").getAsJsonObject();
		paymentDetails = paymentDetails.get("payment").getAsJsonObject();

		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

		log("Setting date in consent config to current date: " + currentDate);
		paymentDetails.addProperty("date", currentDate.toString());
		logSuccess("Successfully added current date to consent config", paymentDetails);

		paymentDetails = paymentDetails.get("details").getAsJsonObject();

		String localInstrument = OIDFJSON.getString(paymentDetails.get("localInstrument"));
		if(!"QRDN".equals(localInstrument)) {
			throw error("LocalInstrument on QRDN consent was not QRDN", Map.of("localInstrument", localInstrument));
		}

		String qrCode = OIDFJSON.getString(paymentDetails.get("qrCode"));
		if(qrCode == null) {
			throw error("Qrcode not present");
		}

		env.putObject("consent_endpoint_request", paymentRequestObject);

		logSuccess(args("consent_endpoint_request", paymentConsent));
		return env;
	}

	private void validate(JsonObject consentConfig) {
		JsonElement element = validate("data", consentConfig);
		validate("loggedUser", element.getAsJsonObject());
		validate("creditor", element.getAsJsonObject());
		validate("payment", element.getAsJsonObject());
	}

	private JsonElement validate(String element, JsonObject object) {
		if(!object.has(element)) {
			throw error("Consent object must have " + element + " field");
		}
		return object.get(element);
	}
}
