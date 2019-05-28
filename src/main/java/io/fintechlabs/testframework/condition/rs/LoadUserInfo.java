package io.fintechlabs.testframework.condition.rs;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class LoadUserInfo extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public LoadUserInfo(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PostEnvironment(required = "user_info")
	public Environment evaluate(Environment env) {

		JsonObject user = new JsonObject();

		user.addProperty("sub", "user-subject-1234531");
		user.addProperty("name", "Demo T. User");
		user.addProperty("email", "user@example.com");
		user.addProperty("email_verified", false);

		env.putObject("user_info", user);

		logSuccess("Added user information", args("user_info", user));

		return env;
	}

}
