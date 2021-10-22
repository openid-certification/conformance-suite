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
		log(obj);
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
		} else {
			logSuccess("Proxy found, still adding the correct proxy for the creditor account");
			obj.addProperty("proxy", "cliente-000000@pix.bcb.gov.br");
			logSuccess("Payment: ",
				env.getObject("resource")
					.getAsJsonObject("brazilPaymentConsent")
					.getAsJsonObject("data")
					.getAsJsonObject("payment")
			);
		}

		log("Ensuring creditor account is correct and in config");
		JsonObject creditorAccount = new JsonObject();
		creditorAccount.addProperty("number", "22917383379");
		creditorAccount.addProperty("accountType", "CACC");
		creditorAccount.addProperty("ispb", "99999060");
		creditorAccount.addProperty("issuer", "0024");

		obj.add("creditorAccount", creditorAccount);
		logSuccess("Added creditorAccount: ", obj);

		log("Updating creditor");
		JsonObject creditor = new JsonObject();
		creditor.addProperty("personType", "PESSOA_NATURAL");
		creditor.addProperty("cpfCnpj", "11122233300");
		creditor.addProperty("name", "Joao Silva");

		obj = env.getObject("resource").getAsJsonObject("brazilPaymentConsent").getAsJsonObject("data");
		obj.add("creditor", creditor);
		logSuccess("Updated creditor: ",
			env.getObject("resource").getAsJsonObject("brazilPaymentConsent").getAsJsonObject("data")
		);
		return env;
	}
}
