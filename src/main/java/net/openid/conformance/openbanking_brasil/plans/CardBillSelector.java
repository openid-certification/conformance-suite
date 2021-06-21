package net.openid.conformance.openbanking_brasil.plans;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CardBillSelector extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String entityString = env.getString("resource_endpoint_response");
		JsonObject accountList = new JsonParser().parse(entityString).getAsJsonObject();
		JsonArray data = accountList.getAsJsonArray("data");
		JsonObject firstAccount = data.get(0).getAsJsonObject();
		String billId = OIDFJSON.getString(firstAccount.get("billId"));
		env.putString("billId", billId);
		return env;
	}

}
