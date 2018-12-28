package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CopyAccessTokenFromASToClient extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public CopyAccessTokenFromASToClient(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = { "access_token", "token_type" })
	@PostEnvironment(required = "access_token")
	public Environment evaluate(Environment env) {

		String accessTokenString = env.getString("access_token");
		String tokenType = env.getString("token_type");

		JsonObject o = new JsonObject();
		o.addProperty("value", accessTokenString);
		o.addProperty("type", tokenType);

		env.putObject("access_token", o);

		logSuccess("Copied the access token", o);

		return env;



	}

}
