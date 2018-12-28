package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractExtractIdToken;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractIdTokenFromTokenResponse extends AbstractExtractIdToken {

	public ExtractIdTokenFromTokenResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "token_endpoint_response")
	@PostEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		return extractIdToken(env, "token_endpoint_response");
	}

}
