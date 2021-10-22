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
			logSuccess("QR code not found, adding one");
			obj.addProperty("qrCode",
				"00020104141234567890123426660014BR.GOV.BCB.PIX014466756C616E6F32303139406578616D706C652E636F6D27300012" +
				"BR.COM.OUTRO011001234567895204000053039865406123.455802BR5915NOMEDORECEBEDOR6008BRASILIA61087007490062" +
				"530515RP12345678-201950300017BR.GOV.BCB.BRCODE01051.0.080450014BR.GOV.BCB.PIX0123PADRAO.URL.PIX/0123AB" +
				"CD81390012BR.COM.OUTRO01190123.ABCD.3456.WXYZ6304EB76");
			logSuccess("details: ", obj);
		} else {
			logSuccess("QR code found.");
			logSuccess("details: ", obj);
		}
		return env;
	}

}
