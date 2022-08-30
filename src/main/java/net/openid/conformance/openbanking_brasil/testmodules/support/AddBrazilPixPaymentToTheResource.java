package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.DictHomologKeys;
import net.openid.conformance.testmodule.Environment;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AddBrazilPixPaymentToTheResource extends AbstractCondition {

	static private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

	@Override
	public Environment evaluate(Environment env) {
		JsonObject consentData;
		JsonObject consentDetails;
		JsonObject consentPayment;
		JsonObject creditorAccount;
		JsonElement localInstrument;
		JsonElement amount;
		JsonElement currency;
		JsonElement type;
		JsonElement proxy;
		JsonElement qrCode;
		JsonElement ibgeTownCode = null;

		JsonObject resource = env.getObject("resource");
		if (resource == null) {
			resource = env.getElementFromObject("config", "resource").getAsJsonObject();
		}
		if (resource == null) {
			throw error("Could not find resource object");
		}

		JsonObject brazilPaymentConsent = resource.getAsJsonObject("brazilPaymentConsent");
		if (brazilPaymentConsent != null) {
			consentData = brazilPaymentConsent.get("data").getAsJsonObject();

			if (consentData != null) {
				consentPayment = consentData.get("payment").getAsJsonObject();
				if (consentPayment != null) {

					amount = consentPayment.get("amount");
					if (amount == null) {
						throw error("amount field is missing in the payment object", consentPayment);
					}

					currency = consentPayment.get("currency");
					if (currency == null) {
						throw error("currency field is missing in the payment object", consentPayment);
					}

					type = consentPayment.get("type");
					if (type == null) {
						throw error("type field is missing in the payment object", consentPayment);
					}

					ibgeTownCode = consentPayment.get("ibgeTownCode");

					consentDetails = consentPayment.getAsJsonObject("details");

					if(consentDetails != null){

						localInstrument = consentDetails.get("localInstrument");
						if (localInstrument == null){
							throw error("localInstrument field is missing in the details", consentDetails);
						}

						creditorAccount = consentDetails.getAsJsonObject("creditorAccount");
						if (creditorAccount == null) {
							throw error("creditorAccount object is missing in the details", consentDetails);
						}

						qrCode = consentDetails.get("qrCode");
						proxy = consentDetails.get("proxy");


					}else {
						throw error("details object is missing in the payment", consentPayment);
					}

				} else {
					throw error("payment object is missing in the data", consentData);
				}
			} else {
				throw error("data object is missing in the brazilPaymentConsent", brazilPaymentConsent);
			}
		} else {
			throw error("brazilPaymentConsent object is missing in the resource", resource);
		}


		JsonElement brazilQrdnCnpj = resource.get("brazilQrdnCnpj");
		if (brazilQrdnCnpj == null) {
			throw error("brazilQrdnCnpj field is missing in the resource");
		}

		JsonObject pixPayment = new JsonObject();
		JsonObject data = new JsonObject();
		JsonObject payment = new JsonObject();

		LocalDateTime currentDateTime = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
		String formattedCurrentDateTime = currentDateTime.format(formatter);

		String endToEndId = String.format("E%s%sabcdef01234", DictHomologKeys.PROXY_E2EID_ISPB, formattedCurrentDateTime);

		pixPayment.add("data", data);
		data.addProperty("endToEndId", endToEndId);
		data.add("payment", payment);
		data.add("creditorAccount", creditorAccount);

		data.add("localInstrument", localInstrument);
		data.addProperty("remittanceInformation", DictHomologKeys.PROXY_EMAIL_STANDARD_REMITTANCEINFORMATION);
		if (qrCode != null) {
			data.add("qrCode", qrCode);
		}
		if (proxy != null) {
			data.add("proxy", proxy);
		}
		data.add("cnpjInitiator", brazilQrdnCnpj);

		if(ibgeTownCode != null) {
			data.add("ibgeTownCode", ibgeTownCode);
		}

		payment.add("amount", amount);
		payment.add("currency", currency);

		env.putObject("resource", "brazilPixPayment", pixPayment);
		logSuccess("Hardcoded brazilPixPayment object was added to the resource", pixPayment);
		return env;
	}
}
