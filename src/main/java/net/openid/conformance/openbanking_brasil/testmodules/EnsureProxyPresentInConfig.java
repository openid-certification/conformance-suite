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
		obj = obj.getAsJsonObject("payment");
		obj = obj.getAsJsonObject("details");
		JsonElement proxy = obj.get("proxy");
		if (proxy == null) {
			logSuccess("Proxy not found, adding one");
			obj.addProperty("proxy", "147459b2-60ad-4503-8ca9-c673cbe6044d");
			logSuccess("Payment: ",
				env.getObject("resource")
					.getAsJsonObject("brazilPaymentConsent")
					.getAsJsonObject("data")
					.getAsJsonObject("payment")
			);
		} else {
			logSuccess("Proxy found.");
			logSuccess("Payment: ",
				env.getObject("resource")
					.getAsJsonObject("brazilPaymentConsent")
					.getAsJsonObject("data")
					.getAsJsonObject("payment")
			);
		}
		return env;
	}
}
