package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureConsentHasNoTransactionFromToDateTime extends AbstractCondition {
	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {
		String dataString = OIDFJSON.getString(env.getElementFromObject("resource_endpoint_response_full", "body"));

		JsonObject data = JsonParser.parseString(dataString).getAsJsonObject();
		JsonObject consent = data.get("data").getAsJsonObject();

		if(consent.has("transactionFromDateTime") || consent.has("transactionToDateTime")) {
			throw  error("transactionFromDateTime and transactionToDateTime can not be in the consent response.");
		}

		logSuccess("transactionFromDateTime and transactionToDateTime not present in the consent response.");
		return env;
	}
}

