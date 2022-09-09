package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddTransactionFromAndToConsentRequestBody extends AbstractCondition {
	@Override
	@PreEnvironment(required = "consent_endpoint_request", strings = {"transactionFromDateTime", "transactionToDateTime"})
	public Environment evaluate(Environment env) {
		JsonObject requestBody = env.getObject("consent_endpoint_request");
		if(!requestBody.has("data")){
			throw error("Could not find data in the consent requestBody", requestBody);
		}

		JsonObject data = requestBody.get("data").getAsJsonObject();

		String transactionToDateTime = env.getString("transactionToDateTime");
		String transactionFromDateTime = env.getString("transactionFromDateTime");

		data.addProperty("transactionFromDateTime", transactionFromDateTime);
		data.addProperty("transactionToDateTime", transactionToDateTime);

		logSuccess("Added transactionFromDateTime  and transactionToDateTime field  to the consent requestBody", requestBody);

		return env;
	}
}
