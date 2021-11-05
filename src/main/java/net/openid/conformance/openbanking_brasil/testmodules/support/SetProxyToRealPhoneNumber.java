package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.DictHomologKeys;
import net.openid.conformance.testmodule.Environment;

public class SetProxyToRealPhoneNumber extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");
		obj = obj.getAsJsonObject("details");
		obj.addProperty("proxy", DictHomologKeys.PROXY_PHONE_NUMBER);

		obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPixPayment");
		obj = obj.getAsJsonObject("data");
		obj.addProperty("proxy", DictHomologKeys.PROXY_PHONE_NUMBER);

		logSuccess("Added phone number as proxy to payment consent and payment payload");

		return env;
	}

}
