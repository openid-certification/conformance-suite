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
public class EnsureMatchingRedirectUri extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public EnsureMatchingRedirectUri(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = { "client", "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {
		// get the client ID from the configuration
		String expected = env.getString("client", "redirect_uri");
		String actual = env.getString("authorization_endpoint_request", "params.redirect_uri");

		if (!Strings.isNullOrEmpty(expected) && expected.equals(actual)) {
			logSuccess("Redirect URI matched",
				args("actual", Strings.nullToEmpty(actual)));
			return env;
		} else {
			throw error("Mismatch between redirect URI", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
		}

	}

}
