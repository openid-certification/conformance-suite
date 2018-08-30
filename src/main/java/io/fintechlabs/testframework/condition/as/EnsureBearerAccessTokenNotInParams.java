package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class EnsureBearerAccessTokenNotInParams extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public EnsureBearerAccessTokenNotInParams(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {
		String incoming = env.getString("incoming_request", "params.access_token");

		if (!Strings.isNullOrEmpty(incoming)) {
			throw error("Found access token on incoming request", args("access_token", incoming));
		} else {
			logSuccess("Couldn't find access token in parameters");
			return env;
		}
	}

}
