package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class SetProxyToFakeEmailAddressOnPayment extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = (JsonObject) env.getElementFromObject("resource", "brazilPixPayment.data");
		obj.addProperty("proxy", "fakeperson@example.com");

		logSuccess("Added non-existent email address as proxy to payment");

		return env;
	}

}
