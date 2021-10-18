package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class ModifyPixPaymentValue extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_request_entity_claims")
	@PostEnvironment(required = "resource_request_entity_claims")
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource_request_entity_claims");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");
		String currentAmount = OIDFJSON.getString(obj.get("amount"));
		String newAmount = (currentAmount.equals("20000.00") ? "21000.00" : "20000.00");
		obj.addProperty("amount", newAmount);
		logSuccess("In order to force request to be a new entity, the payment amount has been modified", Map.of(
			"original amount", currentAmount,
			"new amount", newAmount
		));
		return env;
	}


}
