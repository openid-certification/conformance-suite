package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class SetAmountToBeOverMaxAndCurrencyValid extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject request = env.getObject("consent_endpoint_request");
		log(request);
		JsonObject payment = request.getAsJsonObject("data").getAsJsonObject("payment");
		payment.addProperty("currency", env.getString("old_currency"));
		logSuccess("Successfully reset currency back to normal", payment);
		payment.addProperty("amount", "1000000.00");
		logSuccess("Successfully set amount to max of 1 billion BRL", payment);
		return env;
	}
}
