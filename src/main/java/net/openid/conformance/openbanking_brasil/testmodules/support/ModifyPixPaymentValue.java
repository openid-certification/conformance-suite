package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ModifyPixPaymentValue extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_request_entity_claims")
	@PostEnvironment(required = "resource_request_entity_claims")
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource_request_entity_claims");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");
		String currentAmount = OIDFJSON.getString(obj.get("amount"));
		if(currentAmount.equals("20000,00")) {
			obj.addProperty("amount", "21000.00");
		} else {
			obj.addProperty("amount", "20000.00");
		}
		return env;
	}


}
