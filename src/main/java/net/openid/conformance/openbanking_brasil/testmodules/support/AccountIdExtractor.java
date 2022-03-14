package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AccountIdExtractor extends AbstractCondition {
	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	@PostEnvironment(strings = "accountId")
	public Environment evaluate(Environment env) {
		String entityString = env.getString("resource_endpoint_response");
		if(entityString.equals(null)){
			System.out.println("------------Null------------------------------------------------------");
		}
		else{
			System.out.println("NOT NULL" + entityString+"----------------------------------------------------");
		}
		JsonObject consent = new JsonParser().parse(entityString).getAsJsonObject();
		JsonObject data = consent.getAsJsonObject("data");
		String accountId = OIDFJSON.getString(data.get("accountId"));
		env.putString("accountId", accountId);
		logSuccess("Found account id", args("accountId", accountId));
		return env;
	}

}
