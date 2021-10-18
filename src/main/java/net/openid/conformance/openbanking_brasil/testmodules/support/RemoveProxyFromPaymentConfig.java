package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RemoveProxyFromPaymentConfig extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		log("Removing proxy field from payment config");
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPixPayment");
		obj = obj.getAsJsonObject("data");
		obj.addProperty("proxy", "");
		logSuccess("set proxy in config to be unpopulated");
		return env;
	}

}
