package net.openid.conformance.openbanking_brasil;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ConsentSelector extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String entityString = env.getString("resource_endpoint_response");
		JsonObject consent = new JsonParser().parse(entityString).getAsJsonObject();
		JsonObject data = consent.getAsJsonObject("data");
		String consentId = OIDFJSON.getString(data.get("consentId"));
		env.putString("consentId", consentId);
		return env;
	}

}
