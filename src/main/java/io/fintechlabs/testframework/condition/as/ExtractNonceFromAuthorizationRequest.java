package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ExtractNonceFromAuthorizationRequest extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ExtractNonceFromAuthorizationRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(strings = "nonce")
	public Environment evaluate(Environment env) {

		String nonce = env.getString("authorization_endpoint_request", "params.nonce");

		if (Strings.isNullOrEmpty(nonce)) {
			throw error("Couldn't find 'nonce' in authorization endpoint parameters");
		} else {
			env.putString("nonce", nonce);

			logSuccess("Extracted nonce", args("nonce", nonce));
			return env;
		}

	}

}
