package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class CreatePaymentRequestEntityClaimsFromQrdnConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config" )
	@PostEnvironment(required = "resource_request_entity_claims")
	public Environment evaluate(Environment env) {
		JsonObject paymentConsent = (JsonObject) env.getElementFromObject("resource", "brazilQrdnPaymentConsent");
		String cnpj = OIDFJSON.getString(env.getElementFromObject("resource", "brazilQrdnCnpj"));
		String remittanceInformation = OIDFJSON.getString(env.getElementFromObject("resource", "brazilQrdnRemittance"));
		if(paymentConsent == null) {
			throw error("As 'payments' is included in the 'scope' within the test configuration, a payment consent request JSON object must also be provided in the test configuration.");
		}

		JsonObject paymentData = get(paymentConsent, "data");
		JsonObject paymentObject = get(paymentData, "payment");
		JsonObject paymentDetails = get(paymentObject, "details");
		JsonObject consentCreditorAccount = get(paymentDetails, "creditorAccount");

		JsonObject pixPayment = new JsonObject();
		JsonObject data = new JsonObject();
		data.addProperty("localInstrument", "QRDN");
		data.addProperty("cnpjInitiator", cnpj);
		JsonElement ibgeTownCodeElement = paymentObject.get("ibgeTownCode");
		if(ibgeTownCodeElement != null) {
			data.add("ibgeTownCode", ibgeTownCodeElement);
		}
		data.add("proxy", get(paymentDetails, "proxy"));
		data.add("qrCode", get(paymentDetails, "qrCode"));
		data.addProperty("remittanceInformation", remittanceInformation);

		JsonObject payment = new JsonObject();
		payment.add("amount",get(paymentObject, "amount"));
		payment.add("currency", get(paymentObject, "currency"));

		JsonObject creditorAccount = new JsonObject();
		creditorAccount.add("ispb", get(consentCreditorAccount, "ispb"));
		creditorAccount.add("issuer", get(consentCreditorAccount, "issuer"));
		creditorAccount.add("number", get(consentCreditorAccount, "number"));
		creditorAccount.add("accountType", get(consentCreditorAccount, "accountType"));

		data.add("payment", payment);
		data.add("creditorAccount", creditorAccount);

		pixPayment.add("data", data);
		env.putObject("resource_request_entity_claims", pixPayment);

		logSuccess(args("resource_request_entity_claims", pixPayment));
		return env;
	}

	@SuppressWarnings("unchecked")
	private <J extends JsonElement> J get(JsonObject source, String key) {
		JsonElement element = source.get(key);
		if(element == null) {
			throw error("Unable to find object in JSON", Map.of("object", source, "key", key));
		}
		return (J) element;
	}

}
