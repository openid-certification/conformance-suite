package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class InjectRealCreditorAccountToPayment extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPixPayment");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("creditorAccount");

		obj.addProperty("issuer", "0024");
		obj.addProperty("number", "22917383379");
		obj.addProperty("accountType", "CACC");
		obj.addProperty("ispb", "99999060");
		logSuccess("Added real, working creditor account details to payment");

		return env;
	}
}
