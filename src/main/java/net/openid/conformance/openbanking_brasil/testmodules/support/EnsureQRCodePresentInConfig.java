package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class EnsureQRCodePresentInConfig extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");
		obj = obj.getAsJsonObject("details");
		JsonElement qrCode = obj.get("qrCode");
		if(qrCode == null) {
			throw error("Your payment consent config *must* contain a qrCode in the payment details - This is a test to make sure you reject MANU consents that contain a QR code, and it is therefore required.");
		}
		return env;
	}

}
