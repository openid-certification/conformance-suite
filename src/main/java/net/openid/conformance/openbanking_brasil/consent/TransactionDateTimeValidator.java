package net.openid.conformance.openbanking_brasil.consent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class TransactionDateTimeValidator  extends AbstractJsonAssertingCondition {
	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		String entityString = environment.getString("resource_endpoint_response");
		JsonObject consent = JsonParser.parseString(entityString).getAsJsonObject();
		JsonObject dataObject = consent.getAsJsonObject();

		if(dataObject.get("message") != null) {
			String response = OIDFJSON.getString(dataObject.get("message"));
			log("HERE" + response);
			environment.putString("message", response);
			logSuccess("Response", args("message", response));

			if (response.equals("Bad Request")) {
				logSuccess("400 Response: " + response);
			} else {
				logFailure("Invalid Response: " + response + " Expected 400 Bad Request");
			}
		}
		else {
			logFailure("Invalid Response: Expected 400 Bad Request got Null Pointer Exception");
		}

		return environment;
	}
}
