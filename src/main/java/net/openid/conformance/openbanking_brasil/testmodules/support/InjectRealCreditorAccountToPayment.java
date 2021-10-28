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

		obj.addProperty("issuer", "0001");
		obj.addProperty("number", "1234567");
		obj.addProperty("accountType", "CACC");
		obj.addProperty("ispb", "99999004");
		logSuccess("Added real, working creditor account details to payment");

		return env;
	}
}
