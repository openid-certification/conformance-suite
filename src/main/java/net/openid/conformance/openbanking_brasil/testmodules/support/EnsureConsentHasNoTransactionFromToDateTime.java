package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureConsentHasNoTransactionFromToDateTime extends AbstractCondition {
	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject body = JsonParser.parseString(env.getString("resource_endpoint_response")).getAsJsonObject();

		JsonObject consent = body.get("data").getAsJsonObject();

		if(consent.has("transactionFromDateTime") || consent.has("transactionToDateTime")) {
			throw  error("transactionFromDateTime and transactionToDateTime can not be in the consent response.");
		}

		logSuccess("transactionFromDateTime and transactionToDateTime not present in the consent response.");
		return env;
	}
}

