package io.fintechlabs.testframework.condition.rs;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractBearerAccessTokenFromHeader extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public ExtractBearerAccessTokenFromHeader(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = "incoming_access_token")
	public Environment evaluate(Environment env) {

		String auth = env.getString("incoming_request", "headers.authorization");

		if (!Strings.isNullOrEmpty(auth)) {
			if (auth.toLowerCase().startsWith("bearer")) {
				String incoming = auth.substring("bearer ".length(), auth.length());
				if (!Strings.isNullOrEmpty(incoming)) {
					logSuccess("Found access token on incoming request", args("access_token", incoming));
					env.putString("incoming_access_token", incoming);
					return env;
				} else {
					throw error("Couldn't find access token in header");
				}
			} else {
				throw error("Couldn't find bearer token in authorization header");
			}
		} else {
			throw error("Couldn't find authorization header");
		}

	}

}
