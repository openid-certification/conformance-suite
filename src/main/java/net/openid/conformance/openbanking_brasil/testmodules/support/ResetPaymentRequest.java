package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.math.BigDecimal;

public class ResetPaymentRequest extends AbstractCondition {

	@Override
	public final Environment evaluate(Environment env) {
		log("Resetting consent request");
		JsonObject resource = env.getObject("resource");
		JsonObject payment = resource.getAsJsonObject("brazilPixPayment").getAsJsonObject("data").getAsJsonObject("payment");
		BigDecimal amount = new BigDecimal(OIDFJSON.getString(payment.get("amount")));
		amount = amount.subtract(new BigDecimal(100));

		payment.addProperty("amount", amount.toString());
		logSuccess("Successfully reset payment request", payment);
		if(env.getString("previous_currency") != null){
			payment.addProperty("currency", env.getString("previous_currency"));
		}
		return env;
	}
}
