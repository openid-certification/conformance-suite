package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractAccessTokenFromTokenResponse extends AbstractCondition {

	public ExtractAccessTokenFromTokenResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "token_endpoint_response")
	@PostEnvironment(required = "access_token")
	public Environment evaluate(Environment env) {

		String accessTokenString = env.getString("token_endpoint_response", "access_token");
		if (Strings.isNullOrEmpty(accessTokenString)) {
			throw error("Couldn't find access token");
		}

		String tokenType = env.getString("token_endpoint_response", "token_type");
		if (Strings.isNullOrEmpty(tokenType)) {
			throw error("Couldn't find token type");
		}

		JsonObject o = new JsonObject();
		o.addProperty("value", accessTokenString);
		o.addProperty("type", tokenType);

		env.putObject("access_token", o);

		logSuccess("Extracted the access token", o);

		return env;
	}

}
