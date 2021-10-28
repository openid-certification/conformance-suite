package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class InjectRealCreditorAccountToPaymentConsent extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");

		JsonObject creditor = obj.getAsJsonObject("creditor");
		creditor.addProperty("name", "Joao Silva");
		creditor.addProperty("cpfCnpj", "99991111140");
		creditor.addProperty("personType", "PESSOA_NATURAL");

		obj = obj.getAsJsonObject("payment");
		obj = obj.getAsJsonObject("details");
		obj = obj.getAsJsonObject("creditorAccount");

		obj.addProperty("issuer", "0001");
		obj.addProperty("number", "12345678");
		obj.addProperty("accountType", "CACC");
		obj.addProperty("ispb", "99999004");
		logSuccess("Added real, working creditor account details to payment consent");

		return env;
	}
}
