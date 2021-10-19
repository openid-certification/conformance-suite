package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RemoveProxyFromConsentConfig extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		log("Removing proxy field from consent config");
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");
		obj = obj.getAsJsonObject("details");
		//obj.addProperty("proxy", "");
		obj.remove("proxy");
		logSuccess("set proxy in consent to be unpopulated");
		return env;
	}

}
