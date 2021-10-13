package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ResetPaymentRequest extends AbstractCondition {

	@Override
	public final Environment evaluate(Environment env) {
		log("Resetting consent request");
		JsonObject resource = env.getObject("resource");
		JsonObject payment = resource.getAsJsonObject("brazilPixPayment").getAsJsonObject("data").getAsJsonObject("payment");
		double amount = Double.parseDouble(OIDFJSON.getString(payment.get("amount"))) - 100;
		payment.addProperty("amount", Double.toString(amount));
		logSuccess("Successfully reset payment request", payment);
		if(env.getString("previous_currency") != null){
			payment.addProperty("currency", env.getString("previous_currency"));
		}
		return env;
	}
}
