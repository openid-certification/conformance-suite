package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class EnsureProxyPresentInConfig extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");
		obj = obj.getAsJsonObject("details");
		JsonElement proxy = obj.get("proxy");

		if (proxy == null) {
			logSuccess("Proxy not found, adding one");
			obj.addProperty("proxy", "cliente-000000@pix.bcb.gov.br");
			logSuccess("Payment: ",
				env.getObject("resource")
					.getAsJsonObject("brazilPaymentConsent")
					.getAsJsonObject("data")
					.getAsJsonObject("payment")
			);

			log("Updating creditor account for new proxy value in config");
			JsonObject creditorAccount = new JsonObject();
			creditorAccount.addProperty("number", "22917383379");
			creditorAccount.addProperty("accountType", "CACC");
			creditorAccount.addProperty("ispb", "99999060");
			creditorAccount.addProperty("issuer", "0024");

			obj.add("creditorAccount", creditorAccount);
			logSuccess("Added creditorAccount: ", obj);

			log("Updating creditor for new proxy value in config");
			JsonObject creditor = new JsonObject();
			creditor.addProperty("personType", "PESSOA_NATURAL");
			creditor.addProperty("cpfCnpj", "11122233300");
			creditor.addProperty("name", "Joao Silva");

			obj = env.getObject("resource").getAsJsonObject("brazilPaymentConsent").getAsJsonObject("data");
			obj.add("creditor", creditor);
			logSuccess("Updated creditor: ",
				env.getObject("resource").getAsJsonObject("brazilPaymentConsent").getAsJsonObject("data")
			);

			log("Updating payment config");
			obj = env.getObject("resource");
			obj = obj.getAsJsonObject("brazilPixPayment");
			obj = obj.getAsJsonObject("data");
			obj.add("creditorAccount", creditorAccount);
			obj.addProperty("proxy", "cliente-000000@pix.bcb.gov.br");
			logSuccess("Updated payment config: ", obj);
		}

		return env;
	}
}
