package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.time.Instant;

/**
 * @author jheenan
 */
public class AddExpToRequestObject extends AbstractCondition {

	public AddExpToRequestObject(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		Instant exp = Instant.now().plusSeconds(5 * 60);

		requestObjectClaims.addProperty("exp", exp.getEpochSecond());

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added exp to request object claims", args(
			"exp", requestObjectClaims.getAsJsonPrimitive("exp"))
		);

		return env;
	}
}
