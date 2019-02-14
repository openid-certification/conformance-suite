package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddMultipleAudToRequestObject extends AbstractCondition {
	public AddMultipleAudToRequestObject(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}
	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		requestObjectClaims.remove("aud");

		String serverIssuerUrl = env.getString("server", "issuer");
		JsonArray aud = new JsonArray();
		if (serverIssuerUrl != null) {
			aud.add(serverIssuerUrl);
		}
		aud.add("https://other1.example.com");
		aud.add("invalid");

		requestObjectClaims.add("aud", aud);

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added multiple aud to request object claims", args(
			"aud", requestObjectClaims.getAsJsonArray("aud"))
		);
		return env;
	}
}
