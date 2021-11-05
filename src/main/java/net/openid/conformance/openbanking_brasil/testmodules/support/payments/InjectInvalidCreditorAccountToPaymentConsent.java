package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class InjectInvalidCreditorAccountToPaymentConsent extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");

		JsonObject creditor = obj.getAsJsonObject("creditor");
		creditor.addProperty("name", "Joao Silva");
		creditor.addProperty("cpfCnpj", "99991111141");
		creditor.addProperty("personType", "PESSOA_NATURAL");

		obj = obj.getAsJsonObject("payment");
		obj = obj.getAsJsonObject("details");
		obj = obj.getAsJsonObject("creditorAccount");

		obj.addProperty("issuer", "0001");
		obj.addProperty("number", "12345679");
		obj.addProperty("accountType", "CACC");
		obj.addProperty("ispb", "99999040");
		logSuccess("Added invalid creditor account details to payment consent");

		return env;
	}
}
