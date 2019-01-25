package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class RemoveRedirectUriFromRequestObject extends AbstractCondition {

	public RemoveRedirectUriFromRequestObject(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/*
	 * Remove "redirect_uri" from object "request_object_claims"
	 */
	@Override
	@PreEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		JsonObject req = env.getObject("request_object_claims");

		req.remove("redirect_uri");

		env.putObject("request_object_claims", req);

		logSuccess("Removed redirect_uri from request object claims", args("request_object_claims", req));

		return env;
	}

}
