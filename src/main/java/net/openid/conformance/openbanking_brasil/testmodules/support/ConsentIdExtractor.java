package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ConsentIdExtractor extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	@PostEnvironment(strings = "consent_id")
	public Environment evaluate(Environment env) {
		String entityString = env.getString("resource_endpoint_response");
		JsonObject consent = new JsonParser().parse(entityString).getAsJsonObject();
		JsonObject data = consent.getAsJsonObject("data");
		String consentId = OIDFJSON.getString(data.get("consentId"));
		env.putString("consent_id", consentId);
		logSuccess("Found consent id", args("consentId", consentId));
		return env;
	}

}
