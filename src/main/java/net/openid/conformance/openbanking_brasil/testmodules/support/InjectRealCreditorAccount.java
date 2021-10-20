package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class InjectRealCreditorAccount extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");
		obj = obj.getAsJsonObject("details");
		obj = obj.getAsJsonObject("creditorAccount");

		obj.addProperty("issuer", "0390");
		obj.addProperty("number", "128841");
		obj.addProperty("accountType", "CACC");
		obj.addProperty("ispb", "07237373");
		logSuccess("Added real, working creditor account details to payment consent");

		return env;
	}
}
