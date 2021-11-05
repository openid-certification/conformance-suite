package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class InjectInvalidCreditorAccountToPayment extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPixPayment");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("creditorAccount");

		obj.addProperty("issuer", "0001");
		obj.addProperty("number", "12345679");
		obj.addProperty("accountType", "CACC");
		obj.addProperty("ispb", "99999040");
		logSuccess("Added invalid creditor account details to payment");

		return env;
	}
}
