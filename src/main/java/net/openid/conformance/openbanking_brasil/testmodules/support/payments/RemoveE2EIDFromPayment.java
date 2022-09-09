package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RemoveE2EIDFromPayment extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject resource = env.getObject("resource");
		JsonObject paymentRequest = resource.getAsJsonObject("brazilPixPayment");

		paymentRequest
			.getAsJsonObject("data")
			.remove("endToEndId");

		env.removeObject("endToEndId");

		logSuccess("Successfully removed the endToEndId of the payment request", paymentRequest);

		return env;
	}
}
