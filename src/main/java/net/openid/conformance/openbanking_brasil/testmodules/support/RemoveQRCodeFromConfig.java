package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RemoveQRCodeFromConfig extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject resource = env.getObject("resource");

		JsonObject obj = resource.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");
		obj = obj.getAsJsonObject("details");
		obj.remove("qrCode");

		resource.getAsJsonObject("brazilPixPayment")
			.getAsJsonObject("data")
			.remove("qrCode");

		return env;
	}

}
