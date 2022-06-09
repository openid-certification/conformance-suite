package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.DictHomologKeys;
import net.openid.conformance.testmodule.Environment;

public class AddBrazilPixPaymentToTheResource extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		JsonObject consentData;
		JsonObject consentDetails;
		JsonObject consentPayment;
		JsonObject creditorAccount;
		JsonElement localInstrument;
		JsonElement amount;
		JsonElement currency;
		JsonElement proxy;
		JsonElement qrCode;

		JsonObject resource = env.getObject("resource");
		if (resource == null) {
			resource = env.getElementFromObject("config", "resource").getAsJsonObject();
		}
		if (resource == null) {
			throw error("Could not find resource object");
		}

		JsonObject brazilQrdnPaymentConsent = resource.getAsJsonObject("brazilQrdnPaymentConsent");
		if (brazilQrdnPaymentConsent != null) {
			consentData = brazilQrdnPaymentConsent.get("data").getAsJsonObject();
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
						if (qrCode == null) {
							throw error("qrCode field is missing in the details", consentDetails);
						}

						proxy = consentDetails.get("proxy");
						if (proxy == null) {
							throw error("proxy field is missing in the details", consentDetails);
						}

					}else {
						throw error("details object is missing in the payment", consentPayment);
					}

				} else {
					throw error("payment object is missing in the data", consentData);
				}
			} else {
				throw error("data object is missing in the brazilQrdnPaymentConsent", brazilQrdnPaymentConsent);
			}
		} else {
			throw error("brazilQrdnPaymentConsent object is missing in the resource", resource);
		}


		JsonElement brazilQrdnCnpj = resource.get("brazilQrdnCnpj");
		if (brazilQrdnCnpj == null) {
			throw error("brazilQrdnCnpj field is missing in the resource");
		}

		JsonObject pixPayment = new JsonObject();
		JsonObject data = new JsonObject();
		JsonObject payment = new JsonObject();


		pixPayment.add("data", data);
		data.add("payment", payment);
		data.add("creditorAccount", creditorAccount);

		data.add("localInstrument", localInstrument);
		data.addProperty("remittanceInformation", DictHomologKeys.PROXY_EMAIL_STANDARD_REMITTANCEINFORMATION);
		data.add("qrCode", qrCode);
		data.add("proxy", proxy);
		data.add("cnpjInitiator", brazilQrdnCnpj);
		data.addProperty("ibgeTownCode", DictHomologKeys.PROXY_EMAIL_STANDARD_IBGETOWNCODE);

		payment.add("amount", amount);
		payment.add("currency", currency);

		env.putObject("resource", "brazilPixPayment", pixPayment);
		logSuccess("Hardcoded brazilPixPayment object was added to the resource", pixPayment);
		return env;
	}
}
