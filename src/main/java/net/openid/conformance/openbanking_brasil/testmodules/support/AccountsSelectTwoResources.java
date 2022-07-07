package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AccountsSelectTwoResources extends AbstractCondition {
	@Override
	@PostEnvironment(strings = {"accountId_1","accountId_2"})
	public Environment evaluate(Environment env) {
		String entityString = env.getString("resource_endpoint_response");
		JsonObject accountList = JsonParser.parseString(entityString).getAsJsonObject();

		JsonArray data = accountList.getAsJsonArray("data");
		if(data.isEmpty() || data.size() > 2) {
			throw error("Data field is empty or less than 2 resources have been returned, no further processing required.");
		}

		JsonObject firstAccount = data.get(0).getAsJsonObject();
		log(firstAccount);
		JsonObject secondAccount = data.get(1).getAsJsonObject();
		log(secondAccount);

		String accountIdOne = OIDFJSON.getString(firstAccount.get("accountId"));
		String accountIdTwo = OIDFJSON.getString(secondAccount.get("accountId"));
		env.putString("accountId_1", accountIdOne);
		env.putString("accountId_2", accountIdTwo);
		logSuccess("Specific accounts selected");
		return env;
	}
}
