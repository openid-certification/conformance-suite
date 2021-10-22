package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RemoveTransactionIdentification extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		JsonObject resource = env.getObject("resource");

		JsonObject obj = resource.getAsJsonObject("brazilPixPayment");
		obj = obj.getAsJsonObject("data");
		obj.remove("transactionIdentification");

		logSuccess("Removed transactionidentification from payment", resource.getAsJsonObject("brazilPixPayment"));
		return env;
	}
}
