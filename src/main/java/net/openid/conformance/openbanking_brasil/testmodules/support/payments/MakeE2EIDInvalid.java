package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class MakeE2EIDInvalid extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject resource = env.getObject("resource");
		JsonObject paymentRequest = resource.getAsJsonObject("brazilPixPayment");

		String endToEndId = OIDFJSON.getString(paymentRequest.getAsJsonObject("data").get("endToEndId"));

		StringBuilder sb = new StringBuilder(endToEndId);
		sb.replace(13, 15, "13");

		paymentRequest.getAsJsonObject("data")
			.addProperty("endToEndId", sb.toString());

		env.putString("endToEndId", endToEndId);

		logSuccess("Successfully replace endToEndId by an invalid one", paymentRequest);

		return env;
	}
}
